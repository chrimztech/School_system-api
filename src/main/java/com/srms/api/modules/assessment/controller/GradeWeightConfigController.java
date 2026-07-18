package com.srms.api.modules.assessment.controller;
import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import com.srms.api.modules.assessment.service.GradeWeightConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/schools/{schoolId}/grade-weights") @RequiredArgsConstructor
public class GradeWeightConfigController {
    private final GradeWeightConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<GradeWeightConfig>> get(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(schoolId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<GradeWeightConfig>> update(@PathVariable String schoolId, @RequestBody GradeWeightConfig dto, Authentication auth) {
        String role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", "")).orElse("");
        String actorSchool = auth.getCredentials() == null ? null : auth.getCredentials().toString();
        if (!"SUPER_ADMIN".equals(role) && (!"SCHOOL_ADMIN".equals(role) || !schoolId.equals(actorSchool))) {
            throw new ForbiddenException("Only the school administrator can change grade weights");
        }
        return ResponseEntity.ok(ApiResponse.ok(service.upsert(schoolId, dto.getCaWeight(), dto.getMidtermWeight(), dto.getExamWeight())));
    }
}
