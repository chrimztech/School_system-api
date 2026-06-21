package com.srms.api.modules.assessment.entity;
import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(name = "assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String title;
    private String classId;
    private String className;
    private String subjectId;
    private String subjectName;
    @Enumerated(EnumType.STRING) private AssessmentType type;
    private int maxScore;
    private int weight;
    private LocalDate date;
    private int submitted;
    private int total;
    private String teacherId;
    private boolean published;
    private Integer durationMinutes;
    private String syllabusReference;
    private String gradingScheme;
    private boolean retakeAllowed;
    private String markingCompletedBy;
    public enum AssessmentType { exam, cat, project, homework, quiz, practical }
}
