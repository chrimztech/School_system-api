package com.srms.api.modules.communication.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnnouncementDto {
    private String id;
    private String schoolId;
    private String title;
    private String body;
    private String audience;
    private String channels;
    private LocalDate publishDate;
    private String createdBy;
    private boolean active;
}
