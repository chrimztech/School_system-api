package com.srms.api.modules.backup.repository;

import com.srms.api.modules.backup.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, String> {
    List<Backup> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    Optional<Backup> findByIdAndSchoolId(String id, String schoolId);
    List<Backup> findBySchoolIdAndStatusOrderByCreatedAtDesc(String schoolId, Backup.Status status);
}
