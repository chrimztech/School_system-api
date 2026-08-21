package com.srms.api.modules.accounting.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "fixed_assets", indexes = @Index(name = "idx_fixed_assets_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FixedAsset extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String name;
    private String category;
    @Column(precision = 14, scale = 2) private BigDecimal value;
    private LocalDate purchaseDate;
    private String location;
    private String condition; // New, Good, Fair, Poor, Damaged, Disposed
}
