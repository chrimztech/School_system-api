package com.srms.api.modules.communication.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = @Index(name = "idx_messages_school_id", columnList = "school_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String senderEmail;
    private String senderName;
    private String recipientEmail;
    private String studentId;
    private String studentName;
    private String subject;
    @Column(columnDefinition = "TEXT") private String body;
    private String status; // OPEN, REPLIED, CLOSED
    private LocalDateTime repliedAt;
    @Column(columnDefinition = "TEXT") private String replyBody;
}
