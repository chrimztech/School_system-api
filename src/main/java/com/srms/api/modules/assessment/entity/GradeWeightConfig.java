package com.srms.api.modules.assessment.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "grade_weight_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradeWeightConfig extends BaseEntity {
    @Column(name = "school_id", nullable = false, unique = true) private String schoolId;
    @Builder.Default private int caWeight = 30;
    @Builder.Default private int midtermWeight = 30;
    @Builder.Default private int examWeight = 40;
}
