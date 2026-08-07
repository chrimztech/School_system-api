package com.srms.api.security.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRequestBodyAdviceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void allowsMatchingSchoolIdsIncludingNestedValues() throws Exception {
        assertThatCode(() -> TenantRequestBodyAdvice.assertSchoolIdsAgree(
                objectMapper.readTree("{\"schoolId\":\"school-a\",\"items\":[{\"schoolId\":\"school-a\"}]}"),
                "school-a"))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesChangedSchoolIdInRequestBody() throws Exception {
        assertThatThrownBy(() -> TenantRequestBodyAdvice.assertSchoolIdsAgree(
                objectMapper.readTree("{\"schoolId\":\"school-b\"}"),
                "school-a"))
                .isInstanceOf(ForbiddenException.class);
    }
}
