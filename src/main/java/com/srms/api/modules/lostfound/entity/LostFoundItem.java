package com.srms.api.modules.lostfound.entity;

import com.srms.api.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "lost_found_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LostFoundItem extends BaseEntity {
    @Column(nullable = false) private String schoolId;
    private String itemDescription;
    private String category; // ELECTRONICS, CLOTHING, STATIONERY, BOOK, OTHER
    private String foundLocation;
    private LocalDate foundDate;
    private String foundBy;
    private String ownerName;
    private String ownerContact;
    private String status; // UNCLAIMED, CLAIMED, DISPOSED
    private LocalDate claimedDate;
}
