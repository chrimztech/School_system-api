package com.srms.api.modules.platform.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.platform.dto.PlatformWorkspaceDto;
import com.srms.api.modules.platform.service.PlatformWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/workspace")
@RequiredArgsConstructor
public class PlatformWorkspaceController {
    private final PlatformWorkspaceService platformWorkspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformWorkspaceDto>> getWorkspace() {
        return ResponseEntity.ok(ApiResponse.ok(platformWorkspaceService.getWorkspace()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PlatformWorkspaceDto>> updateWorkspace(@RequestBody PlatformWorkspaceDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(platformWorkspaceService.updateWorkspace(dto)));
    }
}
