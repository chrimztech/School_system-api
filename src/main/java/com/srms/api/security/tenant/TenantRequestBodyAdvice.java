package com.srms.api.security.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class TenantRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        if (inputMessage instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest request = servletRequest.getServletRequest();
            String expectedSchoolId = TenantRequestAttributes.expectedSchoolId(request);
            if (expectedSchoolId != null) {
                assertSchoolIdsAgree(objectMapper.valueToTree(body), expectedSchoolId);
            }
        }
        return body;
    }

    static void assertSchoolIdsAgree(JsonNode node, String expectedSchoolId) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(child -> assertSchoolIdsAgree(child, expectedSchoolId));
            return;
        }
        if (!node.isObject()) return;

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            if (field.getKey().equalsIgnoreCase("schoolId") && value.isTextual()
                    && !value.textValue().isBlank() && !expectedSchoolId.equals(value.textValue())) {
                throw new ForbiddenException("Cross-school request body denied");
            }
            assertSchoolIdsAgree(value, expectedSchoolId);
        }
    }
}
