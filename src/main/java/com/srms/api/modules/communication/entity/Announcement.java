package com.srms.api.modules.communication.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "announcements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String body;
    private String audience; // ALL, PARENTS, TEACHERS, STUDENTS, STAFF
    private String channels; // comma-separated: SMS, EMAIL, WHATSAPP
    private LocalDate publishDate;
    private String createdBy;
    private boolean active = true;
}
