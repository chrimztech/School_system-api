package com.srms.api.modules.communication.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDate;

@Entity
@Table(name = "announcements", indexes = @Index(name = "idx_announcements_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String body;
    private String audience;
    private String channels; // comma-separated: SMS, WhatsApp, Email, USSD
    private LocalDate publishDate;
    private String createdBy;
    private boolean active = true;
    private String priority;      // Normal, Urgent, Emergency
    private String language;      // English, Nyanja, Bemba, Tonga, Lozi
    @ColumnDefault("false")
    private boolean requireAck;
    private String scheduledAt;   // ISO datetime string for scheduled sends
}
