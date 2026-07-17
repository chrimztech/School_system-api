package com.srms.api.modules.canteen.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "canteen_orders", indexes = @Index(name = "idx_canteen_orders_school_id", columnList = "school_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanteenOrder extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String menuItemId;
    private String itemName;
    private Integer quantity;
    private String customerId;
    private String customerType; // STUDENT, STAFF
    private String customerName;
    private LocalDate orderDate;
    private String items;
    @Column(precision = 10, scale = 2) private BigDecimal totalAmount;
    private String paymentMethod;
    private String referenceNumber;
    @Column(columnDefinition = "TEXT") private String notes;
    private String status; // PENDING, PAID, CANCELLED
}
