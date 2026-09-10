package com.srms.api.modules.student.entity;

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
 * A pupil's photo, split out of {@link Student} for the same reason as
 * TeacherSignatureAsset/SchoolBrandingAsset: the student LIST endpoint (GET /students) backs
 * every roster/search/dashboard view in the app and is the single most frequently hit query
 * in the whole system — it previously carried every pupil's base64 photo in every response
 * regardless of whether the caller needed it, only the single-student fetch (profile page,
 * ID card, report card) ever does. Keyed 1:1 by studentId, plain table rather than a JPA
 * relationship (see SchoolBrandingAsset's javadoc for why).
 */
@Entity
@Table(name = "student_photo_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPhotoAsset {
    @Id
    @Column(name = "student_id")
    private String studentId;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;
}
