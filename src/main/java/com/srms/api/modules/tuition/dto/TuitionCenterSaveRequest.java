package com.srms.api.modules.tuition.dto;

import java.util.List;

public record TuitionCenterSaveRequest(
        String name, List<String> subjects, String district, String province, String address,
        String phone, String email, String website, Double rating, String ratingNote, Boolean active) {
}
