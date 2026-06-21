package com.srms.api.modules.communication.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDto {
    private String id;
    private String schoolId;
    private String senderEmail;
    private String senderName;
    private String recipientEmail;
    private String studentId;
    private String studentName;
    private String subject;
    private String body;
    private String status;
    private LocalDateTime repliedAt;
    private String replyBody;
}
