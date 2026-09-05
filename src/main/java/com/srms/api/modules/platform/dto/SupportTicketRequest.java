package com.srms.api.modules.platform.dto;

import lombok.Data;

@Data
public class SupportTicketRequest {
    private String tenantName;
    private String subject;
    private String category;
    private String message;
    private String reporterName;
    private String reporterEmail;
}
