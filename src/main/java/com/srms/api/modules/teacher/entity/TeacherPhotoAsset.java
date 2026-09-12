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
 * A teacher's portrait photo, split out of {@link Teacher} for the same reason as
 * TeacherSignatureAsset — kept off the teacher LIST endpoint's payload, populated only on a
 * single-teacher fetch. Keyed 1:1 by teacherId, plain table rather than a JPA relationship.
 */
@Entity
@Table(name = "teacher_photo_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherPhotoAsset {
    @Id
    @Column(name = "teacher_id")
    private String teacherId;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;
}
