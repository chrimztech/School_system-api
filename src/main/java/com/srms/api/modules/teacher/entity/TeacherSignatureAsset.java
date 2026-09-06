package com.srms.api.modules.teacher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A teacher's signature image, split out of {@link Teacher} for the same reason as
 * SchoolBrandingAsset: the teacher LIST endpoint (GET /teachers) is used to populate ordinary
 * staff-list UIs, and previously carried every teacher's base64 signature (up to ~13MB each) in
 * every response regardless of whether the caller needed it — only the single-teacher fetch
 * (used by the report card's class-teacher lookup and the teacher-profile edit form) ever does.
 * Keyed 1:1 by teacherId, plain table rather than a JPA relationship (see SchoolBrandingAsset's
 * javadoc for why).
 */
@Entity
@Table(name = "teacher_signature_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSignatureAsset {
    @Id
    @Column(name = "teacher_id")
    private String teacherId;

    @Column(columnDefinition = "TEXT")
    private String signatureUrl;
}
