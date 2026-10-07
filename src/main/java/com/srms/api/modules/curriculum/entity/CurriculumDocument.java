package com.srms.api.modules.curriculum.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An uploaded subject syllabus file. The file bytes live on disk; this row is its metadata. */
@Entity
@Table(name = "curriculum_documents", indexes = @Index(name = "idx_curriculum_docs_lookup", columnList = "school_id, subject_name, grade"))
@Getter
@Setter
@NoArgsConstructor
public class CurriculumDocument extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "subject_name", nullable = false) private String subjectName;
    @Column(nullable = false) private Integer grade;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Column(nullable = false) private Integer version;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "content_type") private String contentType;
    @Column(name = "storage_path", nullable = false) private String storagePath;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "uploaded_by") private String uploadedBy;
}
