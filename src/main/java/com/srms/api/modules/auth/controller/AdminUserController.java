package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.service.AuthService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> listAll(Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        return ResponseEntity.ok(ApiResponse.ok(authService.getAllUsers()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createGlobal(
            @RequestBody Map<String, String> body, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        AppUser user = new AppUser();
        user.setEmail(body.get("email"));
        user.setName(body.get("name"));
        user.setSchoolId(body.get("schoolId"));
        user.setActive(true);
        user.setRole(authService.parseRole(body.getOrDefault("role", "SCHOOL_ADMIN")));
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        UserDto created = authService.createUser(user, body.get("password"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        UserDto updated = authService.updateUser(
                userId,
                body.get("role"),
                body.get("schoolId"),
                body.get("phone"),
                body.containsKey("active") ? Boolean.valueOf(body.get("active")) : null,
                body.get("password"),
                body.containsKey("notifyEmail") ? Boolean.valueOf(body.get("notifyEmail")) : null,
                body.containsKey("notifySms") ? Boolean.valueOf(body.get("notifySms")) : null
        );
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String userId, Authentication auth) {
        RoleGuard.requireSuperAdmin(auth);
        authService.deleteUserPermanently(auth.getName(), userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }
}
