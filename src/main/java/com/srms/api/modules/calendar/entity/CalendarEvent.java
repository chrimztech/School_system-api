package com.srms.api.modules.calendar.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "calendar_events") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarEvent extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String eventDate;
    private String title;
    private String category; // Academic, Parents, Sports, Finance, Cultural, Staff, Other
    private String startTime;
    private String endTime;
    private String location;
    private String attendees;
    private String visibility;
    private String owner;
    private String transport;
    @Column(columnDefinition = "TEXT") private String description;
    private int term;
}
