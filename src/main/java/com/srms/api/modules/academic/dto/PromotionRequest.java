package com.srms.api.modules.academic.dto;

import lombok.Data;

import java.util.List;

@Data
public class PromotionRequest {
    private String sourceClassId;
    private String targetAcademicYear;
    private List<Item> items;

    @Data
    public static class Item {
        private String studentId;
        private String enrolmentId;
        private String destinationClassId;
        private boolean graduate;
    }
}
