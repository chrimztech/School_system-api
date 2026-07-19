package com.srms.api.modules.exam.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_candidates",
    indexes = {
        @Index(name = "idx_exam_candidates_school_id", columnList = "school_id"),
        @Index(name = "idx_exam_candidates_exam_paper_id", columnList = "exam_paper_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_candidates_paper_student", columnNames = {"exam_paper_id", "student_id"}),
        @UniqueConstraint(name = "uq_exam_candidates_paper_gce", columnNames = {"exam_paper_id", "gce_candidate_id"})
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamCandidate extends BaseEntity {
    @Column(name = "school_id", nullable = false) private String schoolId;
    @Column(name = "exam_paper_id", nullable = false) private String examPaperId;
    @Column(name = "candidate_type", nullable = false) private String candidateType; // INTERNAL, GCE
    @Column(name = "student_id") private String studentId;
    @Column(name = "gce_candidate_id") private String gceCandidateId;
    private String candidateName;
    private String grade;
    private Integer seatNumber;
    @Builder.Default private String status = "REGISTERED";
}
