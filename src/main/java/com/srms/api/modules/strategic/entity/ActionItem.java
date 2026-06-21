package com.srms.api.modules.strategic.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "strategic_action_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActionItem extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String goalId;
    @Column(nullable = false, length = 300) private String action;
    private String owner;
    private String dueDate;
    private String priority;
    private String status;
}
