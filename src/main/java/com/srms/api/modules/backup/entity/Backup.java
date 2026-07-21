package com.srms.api.modules.backup.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A point-in-time export of one school's rows across every table that carries a school_id
 * column — never a raw pg_dump, since this app is multi-tenant on a single shared database
 * and a whole-database dump taken from a per-school action would leak every other school's
 * data to whoever downloads it.
 */
@Entity
@Table(name = "backups", indexes = @Index(name = "idx_backups_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Backup extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "file_name") private String fileName;
    @Column(name = "file_path") private String filePath;
    @Column(name = "size_bytes") private long sizeBytes;
    @Column(name = "table_count") private int tableCount;
    @Column(name = "row_count") private long rowCount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false)
    private TriggeredBy triggeredBy;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    public enum Status { IN_PROGRESS, COMPLETED, FAILED }
    public enum TriggeredBy { MANUAL, SCHEDULED }
}
