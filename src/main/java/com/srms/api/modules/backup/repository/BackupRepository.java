package com.srms.api.modules.backup.repository;

import com.srms.api.modules.backup.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, String> {
    List<Backup> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    Optional<Backup> findByIdAndSchoolId(String id, String schoolId);
    List<Backup> findBySchoolIdAndStatusOrderByCreatedAtDesc(String schoolId, Backup.Status status);

    // Used to make the nightly scheduled-backup cron idempotent across instances: if this app
    // ever runs behind a load balancer with more than one backend instance, every instance's
    // in-process @Scheduled cron fires at the same wall-clock time (there's no distributed lock
    // here — see BackupService.runScheduledBackups) — this check is what stops that from
    // producing one duplicate backup per school per extra instance.
    boolean existsBySchoolIdAndTriggeredByAndCreatedAtAfter(String schoolId, Backup.TriggeredBy triggeredBy, LocalDateTime after);
}
