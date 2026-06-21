package com.srms.api.modules.canteen.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "menu_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItem extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    @Column(nullable = false) private String name;
    private String category; // MAIN, SNACK, DRINK, SPECIAL
    @Column(precision = 10, scale = 2) private BigDecimal price;
    @Column(precision = 10, scale = 2) private BigDecimal costPrice;
    private boolean available = true;
    private Integer openingStock;
    private Integer reorderLevel;
    private String servingSize;
    private String unitOfMeasure;
    private String issuePoint;
    private String allergens;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean vegetarian = false;
    @Builder.Default
    @Column(columnDefinition = "boolean default false") private Boolean halal = false;
    private String supplier;
    private Integer preparationTime;
    private String description;
}
