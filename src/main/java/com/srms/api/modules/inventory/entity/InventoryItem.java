package com.srms.api.modules.inventory.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String itemCode;
    @Column(nullable = false) private String name;
    private String category; // STATIONERY, FURNITURE, ELECTRONICS, SPORTS, CLEANING, OTHER
    private String unit;
    private int quantityInStock;
    private int reorderLevel;
    @Column(precision = 12, scale = 2) private BigDecimal unitCost;
    private String location;
    private LocalDate lastRestockedDate;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
}
