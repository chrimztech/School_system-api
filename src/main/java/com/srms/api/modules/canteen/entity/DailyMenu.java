package com.srms.api.modules.canteen.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity @Table(name = "daily_menus", indexes = @Index(name = "idx_daily_menus_school_date", columnList = "school_id, menu_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyMenu extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(name = "menu_date", nullable = false) private LocalDate menuDate;
    @Column(nullable = false) private String mealPeriod; // BREAKFAST, LUNCH, SUPPER, SNACK
    @Column(nullable = false, columnDefinition = "text") private String items;
    private String notes;
    private String enteredBy;
}
