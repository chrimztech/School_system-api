package com.srms.api.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BulkImportResult {
    private int imported;
    private List<RowError> errors;
    // Row index (into the request's dto list) -> id of the record that row created. Only
    // populated by callers that need the frontend to act on the new records afterward (e.g.
    // StudentService.bulkCreate, so a CSV import can offer to auto-enrol the imported pupils
    // into a matching class) — empty for callers that don't.
    private List<CreatedRow> created = new ArrayList<>();

    public BulkImportResult(int imported, List<RowError> errors) {
        this(imported, errors, new ArrayList<>());
    }

    public BulkImportResult(int imported, List<RowError> errors, List<CreatedRow> created) {
        this.imported = imported;
        this.errors = errors;
        this.created = created;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedRow {
        private int row;
        private String id;
    }
}
