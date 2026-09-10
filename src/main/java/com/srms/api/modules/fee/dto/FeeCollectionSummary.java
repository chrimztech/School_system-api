package com.srms.api.modules.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeeCollectionSummary {
    private double collected;
    private double outstanding;
    private double collectionRate;
}
