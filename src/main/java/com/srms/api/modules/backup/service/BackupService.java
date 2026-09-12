package com.srms.api.modules.backup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.backup.entity.Backup;
import com.srms.api.modules.backup.repository.BackupRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Exports and restores one school's data at a time by walking every table that carries a
 * school_id column (discovered from information_schema, not hard-coded) rather than shelling
 * out to pg_dump — a raw pg_dump would include every other tenant's rows too, since this app
 * keeps all schools in one shared database distinguished only by a school_id column.
 */
@Service
@RequiredArgsConstructor
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    // Never part of a "restore this school to a point in time" operation: audit_events is a
    // historical log (restoring it would rewrite history), and backups is this module's own
    // bookkeeping — including it in its own tenant-data walk means restoring any backup would
    // delete-then-reinsert the backups table itself, destroying every other snapshot's record.
    // payment_callback_logs is the same story (an immutable trail of raw payment-gateway
    // callbacks) but never needs listing here — it has no school_id column at all, so the
    // information_schema walk below never finds it in the first place.
    private static final Set<String> EXCLUDED_TABLES = Set.of("backups", "audit_events");

    // The school's own row (name, logo/favicon, branding, banking, subscription/plan, settings —
    // School.java) is keyed by its own "id", not "school_id", so it never surfaces from the
    // school_id-column walk below and has to be captured/restored separately.
    private static final String SCHOOL_TABLE = "schools";

    /** Sentinel Backup.schoolId for a whole-system snapshot's platform-wide-tables entry —
     * everything that has no school_id column at all (app_users, platform_workspace,
     * platform_integration_configs, ...), captured once per full-system backup rather than
     * once per school. Reuses the entire existing Backup entity/repository/download/delete
     * machinery; schoolId here is a plain unconstrained string column, not a real school. */
    public static final String PLATFORM_SCOPE = "__PLATFORM__";

    // Excluded from the platform-tables walk for the same reasons EXCLUDED_TABLES excludes them
    // from the per-school walk, plus flyway's own bookkeeping table (never meaningful to
    // snapshot/restore) and every school_id-scoped table (already captured per-school).
    private static final Set<String> PLATFORM_EXTRA_EXCLUDED = Set.of("flyway_schema_history");

    private final BackupRepository backupRepository;
    private final SchoolRepository schoolRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.backup.storage-dir:./backups}")
    private String storageDir;

    @Value("${app.backup.retention-count:14}")
    private int retentionCount;

    public List<Backup> list(String schoolId) {
        return backupRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    public Backup get(String schoolId, String backupId) {
        return backupRepository.findByIdAndSchoolId(backupId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", backupId));
    }

    public Backup createBackup(String schoolId, String actorName, Backup.TriggeredBy triggeredBy) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", schoolId));

        Backup backup = new Backup();
        backup.setSchoolId(school.getId());
        backup.setStatus(Backup.Status.IN_PROGRESS);
        backup.setTriggeredBy(triggeredBy);
        backup.setCreatedBy(actorName);
        backup = backupRepository.save(backup);

        try {
            List<String> tables = tenantTableNames();
            Map<String, List<Map<String, Object>>> dump = new LinkedHashMap<>();
            long rowCount = 0;
            for (String table : tables) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT * FROM \"" + table + "\" WHERE school_id = ?", school.getId());
                dump.put(table, rows);
                rowCount += rows.size();
            }
            List<Map<String, Object>> schoolRow = jdbcTemplate.queryForList(
                    "SELECT * FROM \"" + SCHOOL_TABLE + "\" WHERE id = ?", school.getId());
            dump.put(SCHOOL_TABLE, schoolRow);
            rowCount += schoolRow.size();

            Path dir = Paths.get(storageDir, school.getId());
            Files.createDirectories(dir);
            String fileName = FILE_STAMP.format(LocalDateTime.now()) + "_" + backup.getId() + ".json.gz";
            Path file = dir.resolve(fileName);
            try (var out = Files.newOutputStream(file); var gzip = new GZIPOutputStream(out)) {
                objectMapper.writeValue(gzip, dump);
            }

            backup.setFileName(fileName);
            backup.setFilePath(file.toAbsolutePath().toString());
            backup.setSizeBytes(Files.size(file));
            backup.setTableCount(tables.size() + 1);
            backup.setRowCount(rowCount);
            backup.setStatus(Backup.Status.COMPLETED);
            backup.setCompletedAt(LocalDateTime.now());
            backup = backupRepository.save(backup);
        } catch (Exception e) {
            log.error("Backup failed for school {}", schoolId, e);
            backup.setStatus(Backup.Status.FAILED);
            backup.setErrorMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            backup.setCompletedAt(LocalDateTime.now());
            backupRepository.save(backup);
            throw new BusinessException("Backup failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }

        // Retention cleanup is best-effort and must never retroactively mark the backup that
        // just succeeded as failed — keep it outside the try/catch above.
        try {
            applyRetention(school.getId());
        } catch (Exception e) {
            log.warn("Retention cleanup failed for school {}", schoolId, e);
        }
        return backup;
    }

    public byte[] readFile(String schoolId, String backupId) {
        Backup backup = get(schoolId, backupId);
        if (backup.getStatus() != Backup.Status.COMPLETED || backup.getFilePath() == null) {
            throw new BusinessException("This backup did not complete successfully and has no file to download");
        }
        try {
            return Files.readAllBytes(Paths.get(backup.getFilePath()));
        } catch (IOException e) {
            throw new BusinessException("Backup file is missing from storage: " + e.getMessage());
        }
    }

    @Transactional
    public void restore(String schoolId, String backupId) {
        Backup backup = get(schoolId, backupId);
        if (backup.getStatus() != Backup.Status.COMPLETED || backup.getFilePath() == null) {
            throw new BusinessException("Only a completed backup can be restored");
        }

        Map<String, List<Map<String, Object>>> dump;
        try (InputStream in = Files.newInputStream(Paths.get(backup.getFilePath()));
             GZIPInputStream gzip = new GZIPInputStream(in)) {
            dump = objectMapper.readValue(gzip, new TypeReference<>() {});
        } catch (IOException e) {
            throw new BusinessException("Could not read backup file: " + e.getMessage());
        }
        applyDump(schoolId, dump);
    }

    /**
     * Restores from a backup file the caller uploads, rather than one already tracked in this
     * server's own {@code backups} table/disk — the path an admin needs after downloading a
     * snapshot to move it between environments, or restoring from an off-site copy kept after
     * the original row was pruned by retention.
     */
    @Transactional
    public void importAndRestore(String schoolId, InputStream uploadStream) {
        Map<String, List<Map<String, Object>>> dump;
        try (GZIPInputStream gzip = new GZIPInputStream(uploadStream)) {
            dump = objectMapper.readValue(gzip, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException("This doesn't look like a valid backup file — expected a .json.gz export produced by this system.");
        }
        if (dump == null || dump.isEmpty()) {
            throw new BusinessException("This backup file has no data to restore.");
        }
        List<String> currentTenantTables = tenantTableNames();
        if (dump.keySet().stream().noneMatch(currentTenantTables::contains)) {
            throw new BusinessException("This file doesn't match any known table in this system — it may not be a backup export.");
        }
        applyDump(schoolId, dump);
    }

    private void applyDump(String schoolId, Map<String, List<Map<String, Object>>> dump) {
        List<Map<String, Object>> schoolRow = dump.get(SCHOOL_TABLE);
        if (schoolRow != null && !schoolRow.isEmpty()) {
            restoreSchoolRow(schoolId, schoolRow.get(0));
        }

        List<String> currentTenantTables = tenantTableNames();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dump.entrySet()) {
            String table = entry.getKey();
            if (SCHOOL_TABLE.equals(table)) continue; // handled above — keyed by "id", not "school_id"
            if (!currentTenantTables.contains(table)) continue; // table renamed/dropped since this backup was taken
            restoreTableRows(table, entry.getValue(), schoolId);
        }
    }

    /** Deletes then re-inserts one table's rows from a backup dump. When forcedSchoolId is
     * non-null (restoring one school's tenant-scoped table), only that school's rows are
     * touched and "school_id" is always forced to the target rather than trusted from the
     * file — an uploaded import isn't guaranteed to have come from this same schoolId, and
     * inserting rows under a different tenant's id would leak data across schools. When null
     * (restoring a platform-wide table that has no school_id column at all), the whole table
     * is replaced. */
    private void restoreTableRows(String table, List<Map<String, Object>> rows, String forcedSchoolId) {
        if (forcedSchoolId != null) {
            jdbcTemplate.update("DELETE FROM \"" + table + "\" WHERE school_id = ?", forcedSchoolId);
        } else {
            jdbcTemplate.update("DELETE FROM \"" + table + "\"");
        }
        if (rows.isEmpty()) return;

        Map<String, Integer> currentColumns = tableColumnTypes(table);
        for (Map<String, Object> row : rows) {
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, Object> col : row.entrySet()) {
                Integer sqlType = currentColumns.get(col.getKey());
                if (sqlType == null) continue; // column dropped since backup
                columns.add(col.getKey());
                values.add(forcedSchoolId != null && "school_id".equals(col.getKey())
                        ? forcedSchoolId : coerceForColumn(col.getValue(), sqlType));
            }
            if (columns.isEmpty()) continue;
            String placeholders = String.join(",", columns.stream().map(c -> "?").toList());
            String columnList = columns.stream().map(c -> "\"" + c + "\"").reduce((a, b) -> a + "," + b).orElse("");
            jdbcTemplate.update(
                    "INSERT INTO \"" + table + "\" (" + columnList + ") VALUES (" + placeholders + ")",
                    values.toArray());
        }
    }

    /** Applies a backed-up {@code schools} row via UPDATE rather than the generic delete/insert
     * used for every other table — that row is found by primary key ("id"), not "school_id",
     * and deleting it would cascade-break every other table's foreign key back to it. */
    private void restoreSchoolRow(String schoolId, Map<String, Object> row) {
        Map<String, Integer> currentColumns = tableColumnTypes(SCHOOL_TABLE);
        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> col : row.entrySet()) {
            if ("id".equals(col.getKey())) continue; // never move this row under a different primary key
            Integer sqlType = currentColumns.get(col.getKey());
            if (sqlType == null) continue; // column dropped since backup
            setClauses.add("\"" + col.getKey() + "\" = ?");
            values.add(coerceForColumn(col.getValue(), sqlType));
        }
        if (setClauses.isEmpty()) return;
        values.add(schoolId);
        jdbcTemplate.update("UPDATE \"" + SCHOOL_TABLE + "\" SET " + String.join(",", setClauses) + " WHERE id = ?", values.toArray());
    }

    public void delete(String schoolId, String backupId) {
        Backup backup = get(schoolId, backupId);
        deleteInternal(backup);
    }

    private void deleteInternal(Backup backup) {
        if (backup.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(backup.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete backup file {}", backup.getFilePath(), e);
            }
        }
        backupRepository.delete(backup);
    }

    // ── Platform-wide (System Backup) ──────────────────────────────────────────────────────
    // Everything below operates across every school at once, or on the platform-scoped tables
    // that have no school_id column at all — super-admin only, see PlatformBackupController.

    /** Every backup across every school plus every platform-tables entry, newest first — the
     * consolidated view the System Backup page needs instead of picking one school at a time. */
    public List<Backup> listAll() {
        return backupRepository.findAllByOrderByCreatedAtDesc();
    }

    public Backup getById(String backupId) {
        return backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", backupId));
    }

    public byte[] readFileById(String backupId) {
        Backup backup = getById(backupId);
        if (backup.getStatus() != Backup.Status.COMPLETED || backup.getFilePath() == null) {
            throw new BusinessException("This backup did not complete successfully and has no file to download");
        }
        try {
            return Files.readAllBytes(Paths.get(backup.getFilePath()));
        } catch (IOException e) {
            throw new BusinessException("Backup file is missing from storage: " + e.getMessage());
        }
    }

    public void deleteById(String backupId) {
        deleteInternal(getById(backupId));
    }

    /** One backup per active school (reusing createBackup exactly as-is) plus one additional
     * entry for every platform-wide table — a school that fails doesn't stop the others; its
     * own backup row just ends up FAILED with the error recorded, same as any single-school
     * backup failure would. */
    public List<Backup> createFullSystemBackup(String actorName, Backup.TriggeredBy triggeredBy) {
        List<Backup> created = new ArrayList<>();
        for (School school : schoolRepository.findByActiveTrue()) {
            try {
                created.add(createBackup(school.getId(), actorName, triggeredBy));
            } catch (Exception e) {
                log.error("Full-system backup: school {} failed", school.getId(), e);
            }
        }
        created.add(createPlatformTablesBackup(actorName, triggeredBy));
        return created;
    }

    public Backup createPlatformTablesBackup(String actorName, Backup.TriggeredBy triggeredBy) {
        Backup backup = new Backup();
        backup.setSchoolId(PLATFORM_SCOPE);
        backup.setStatus(Backup.Status.IN_PROGRESS);
        backup.setTriggeredBy(triggeredBy);
        backup.setCreatedBy(actorName);
        backup = backupRepository.save(backup);

        try {
            List<String> tables = platformTableNames();
            Map<String, List<Map<String, Object>>> dump = new LinkedHashMap<>();
            long rowCount = 0;
            for (String table : tables) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM \"" + table + "\"");
                dump.put(table, rows);
                rowCount += rows.size();
            }

            Path dir = Paths.get(storageDir, PLATFORM_SCOPE);
            Files.createDirectories(dir);
            String fileName = FILE_STAMP.format(LocalDateTime.now()) + "_" + backup.getId() + ".json.gz";
            Path file = dir.resolve(fileName);
            try (var out = Files.newOutputStream(file); var gzip = new GZIPOutputStream(out)) {
                objectMapper.writeValue(gzip, dump);
            }

            backup.setFileName(fileName);
            backup.setFilePath(file.toAbsolutePath().toString());
            backup.setSizeBytes(Files.size(file));
            backup.setTableCount(tables.size());
            backup.setRowCount(rowCount);
            backup.setStatus(Backup.Status.COMPLETED);
            backup.setCompletedAt(LocalDateTime.now());
            return backupRepository.save(backup);
        } catch (Exception e) {
            log.error("Platform-tables backup failed", e);
            backup.setStatus(Backup.Status.FAILED);
            backup.setErrorMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            backup.setCompletedAt(LocalDateTime.now());
            backupRepository.save(backup);
            throw new BusinessException("Platform-tables backup failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    @Transactional
    public void restorePlatformTables(String backupId) {
        Backup backup = getById(backupId);
        if (!PLATFORM_SCOPE.equals(backup.getSchoolId())) {
            throw new BusinessException("This backup is not a platform-tables snapshot");
        }
        if (backup.getStatus() != Backup.Status.COMPLETED || backup.getFilePath() == null) {
            throw new BusinessException("Only a completed backup can be restored");
        }
        Map<String, List<Map<String, Object>>> dump;
        try (InputStream in = Files.newInputStream(Paths.get(backup.getFilePath()));
             GZIPInputStream gzip = new GZIPInputStream(in)) {
            dump = objectMapper.readValue(gzip, new TypeReference<>() {});
        } catch (IOException e) {
            throw new BusinessException("Could not read backup file: " + e.getMessage());
        }
        List<String> currentPlatformTables = platformTableNames();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dump.entrySet()) {
            if (!currentPlatformTables.contains(entry.getKey())) continue; // table renamed/dropped since this backup was taken
            restoreTableRows(entry.getKey(), entry.getValue(), null);
        }
    }

    /** Every base table in the public schema that does NOT carry a school_id column — the
     * inverse of tenantTableNames() — excluding this module's own bookkeeping table and
     * Flyway's migration history, neither of which is ever meaningful to snapshot/restore. */
    private List<String> platformTableNames() {
        List<String> allTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class);
        Set<String> schoolScoped = Set.copyOf(tenantTableNames());
        return allTables.stream()
                .filter(t -> !schoolScoped.contains(t))
                .filter(t -> !EXCLUDED_TABLES.contains(t) && !PLATFORM_EXTRA_EXCLUDED.contains(t))
                .toList();
    }

    /**
     * Nightly export for every active school — matches what the Backups page promises.
     *
     * There's no distributed lock here: if this app is ever deployed behind a load balancer
     * with more than one backend instance, every instance's own in-process @Scheduled cron
     * fires independently at 2am in each instance's JVM. The per-school "already ran within
     * the last 12 hours" check below is what keeps that from producing one duplicate backup
     * per school per extra instance — cheap and correct given backups already record when
     * they ran, versus pulling in a distributed-lock dependency (e.g. ShedLock) for a single
     * nightly job.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Harare")
    public void runScheduledBackups() {
        LocalDateTime since = LocalDateTime.now().minusHours(12);
        for (School school : schoolRepository.findByActiveTrue()) {
            if (backupRepository.existsBySchoolIdAndTriggeredByAndCreatedAtAfter(school.getId(), Backup.TriggeredBy.SCHEDULED, since)) {
                continue;
            }
            try {
                createBackup(school.getId(), "System", Backup.TriggeredBy.SCHEDULED);
            } catch (Exception e) {
                log.error("Scheduled backup failed for school {}", school.getId(), e);
            }
        }
    }

    private void applyRetention(String schoolId) {
        List<Backup> completed = backupRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, Backup.Status.COMPLETED);
        if (completed.size() <= retentionCount) return;
        for (Backup stale : completed.subList(retentionCount, completed.size())) {
            delete(schoolId, stale.getId());
        }
    }

    private List<String> tenantTableNames() {
        List<String> all = jdbcTemplate.queryForList(
                "SELECT DISTINCT table_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND column_name = 'school_id' ORDER BY table_name",
                String.class);
        return all.stream().filter(t -> !EXCLUDED_TABLES.contains(t)).toList();
    }

    /** Column name -> java.sql.Types code, read straight from JDBC metadata so restore can
     * bind each value with its real SQL type instead of whatever generic type Jackson guessed
     * when it deserialized the backup's JSON (which flattens everything that isn't a number,
     * boolean, or plain string — timestamps included — down to a String). */
    private Map<String, Integer> tableColumnTypes(String table) {
        Map<String, Integer> types = new LinkedHashMap<>();
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, "public", table, null)) {
            while (rs.next()) {
                types.put(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE"));
            }
        } catch (SQLException e) {
            throw new BusinessException("Could not read column metadata for " + table + ": " + e.getMessage());
        }
        return types;
    }

    /** Jackson deserializes a backed-up timestamp/date/time value back into a plain ISO-8601
     * String (there's no type hint in a Map&lt;String,Object&gt; to tell it otherwise), and the
     * JDBC driver won't implicitly cast a String parameter to those column types on INSERT. */
    private Object coerceForColumn(Object value, int sqlType) {
        if (!(value instanceof String s) || s.isBlank()) return value;
        try {
            return switch (sqlType) {
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> Timestamp.valueOf(parseDateTime(s));
                case Types.DATE -> java.sql.Date.valueOf(LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s));
                case Types.TIME -> java.sql.Time.valueOf(LocalTime.parse(s));
                default -> value;
            };
        } catch (Exception e) {
            return value; // best effort — let the driver report its own error if this truly doesn't fit
        }
    }

    private LocalDateTime parseDateTime(String s) {
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.parse(s);
        }
    }
}
