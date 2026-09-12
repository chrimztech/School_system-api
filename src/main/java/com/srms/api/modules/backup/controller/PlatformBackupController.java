package com.srms.api.modules.backup.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.backup.entity.Backup;
import com.srms.api.modules.backup.service.BackupService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Whole-system backup and restore — every school's data plus every platform-wide table
 * (app_users, platform_workspace, platform_integration_configs, ...) in one operation, rather
 * than one school at a time via /api/schools/{schoolId}/backups. Super-admin only: this reads
 * and can overwrite every tenant's data at once. */
@RestController
@RequestMapping("/api/platform/backups")
@RequiredArgsConstructor
public class PlatformBackupController {
    private final BackupService backupService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Backup>>> list(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(backupService.listAll()));
    }

    /** Kicks off one backup per active school plus one platform-tables snapshot. Runs
     * synchronously — for a platform with many schools this call can take a while, same
     * trade-off the per-school "Run backup now" button already makes. */
    @PostMapping("/full")
    public ResponseEntity<ApiResponse<List<Backup>>> createFull(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        List<Backup> created = backupService.createFullSystemBackup(actorName(auth), Backup.TriggeredBy.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        Backup backup = backupService.getById(id);
        byte[] file = backupService.readFileById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(backup.getFileName()).build().toString())
                .body(file);
    }

    /** Restores the one platform-tables entry from a whole-system backup. To restore an
     * individual school from the same backup set, use the existing
     * POST /api/schools/{schoolId}/backups/{id}/restore — this endpoint only ever touches
     * tables with no school_id column. */
    @PostMapping("/{id}/restore-platform-tables")
    public ResponseEntity<ApiResponse<Void>> restorePlatformTables(@PathVariable String id, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        backupService.restorePlatformTables(id);
        return ResponseEntity.ok(ApiResponse.ok("Platform tables restored", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        backupService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Backup deleted", null));
    }

    private String actorName(Authentication auth) {
        return userRepository.findById(auth.getName())
                .map(AppUser::getName)
                .orElse(auth.getName());
    }
}
