package com.srms.api.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResult {
    private int imported;
    private List<RowError> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String error;
    }
}
