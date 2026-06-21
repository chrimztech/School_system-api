package com.srms.api.modules.inventory.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String itemId;
    private String itemName;
    private String movementType; // IN, OUT, ADJUSTMENT
    private int quantity;
    private String reason;
    private String performedBy;
    private LocalDate movementDate;
}
