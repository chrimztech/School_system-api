package com.srms.api.modules.tuition.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One entry in the platform-wide tuition centre directory, used to recommend extra help near a
 * school for subjects a pupil is struggling in. Deliberately not school-scoped: a good centre
 * serves every school nearby, not just one — see PerformanceInsightService for how it's matched
 * to a pupil's weak subjects and TuitionCenterService for how "closest" is ranked (same district,
 * then same province, then rating).
 */
@Entity
@Table(name = "tuition_centers")
@Getter
@Setter
@NoArgsConstructor
public class TuitionCenter extends BaseEntity {
    @Column(nullable = false) private String name;
    /** Comma-separated subject names (e.g. "Mathematics, Physics"). Kept as free text rather
     * than a child table — the directory is small enough that in-memory matching is simpler and
     * just as fast, and it avoids a join table for something an admin edits as a single list. */
    @Column(name = "subjects_text", nullable = false, columnDefinition = "TEXT") private String subjectsText;
    private String district;
    private String province;
    private String address;
    private String phone;
    private String email;
    private String website;
    /** Admin-entered quality score, 0-5. Not yet backed by parent reviews — see ratingNote for
     * where the number comes from until a real review system exists. */
    private Double rating;
    @Column(name = "rating_note", columnDefinition = "TEXT") private String ratingNote;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "updated_by") private String updatedBy;
}
