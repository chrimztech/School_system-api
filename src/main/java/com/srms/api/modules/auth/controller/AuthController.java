package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.dto.AuthResponse;
import com.srms.api.modules.auth.dto.LoginRequest;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.service.AuthService;
import com.srms.api.security.tenant.TenantRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.login(request, TenantRequestAttributes.resolution(servletRequest))));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @RequestBody Map<String, String> body,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.loginWithGoogle(body.get("idToken"), TenantRequestAttributes.resolution(servletRequest))));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(auth.getName())));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateMe(Authentication auth, @RequestBody Map<String, Object> body) {
        String phone = body.containsKey("phone") ? String.valueOf(body.get("phone")) : null;
        Boolean notifyEmail = body.containsKey("notifyEmail") ? Boolean.valueOf(String.valueOf(body.get("notifyEmail"))) : null;
        Boolean notifySms = body.containsKey("notifySms") ? Boolean.valueOf(String.valueOf(body.get("notifySms"))) : null;
        return ResponseEntity.ok(ApiResponse.ok(authService.updateOwnProfile(auth.getName(), phone, notifyEmail, notifySms)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        authService.changePassword(
                auth.getName(),
                body.get("currentPassword"),
                body.get("newPassword")
        );
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
