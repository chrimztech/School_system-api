package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.service.AuthService;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> list(@PathVariable String schoolId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        return ResponseEntity.ok(ApiResponse.ok(authService.getUsersBySchool(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> create(
            @PathVariable String schoolId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        if ("SUPER_ADMIN".equalsIgnoreCase(body.get("role"))) {
            throw new ForbiddenException("Platform administrators must be created from the platform workspace");
        }

        AppUser user = new AppUser();
        user.setEmail(body.get("email"));
        user.setName(body.get("name"));
        user.setSchoolId(schoolId);
        user.setActive(true);
        user.setRole(authService.parseRole(body.getOrDefault("role", "SCHOOL_ADMIN")));

        if (body.containsKey("phone")) user.setPhone(body.get("phone"));

        UserDto created = authService.createUser(user, body.get("password"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable String schoolId,
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        assertTargetBelongsToSchool(schoolId, userId, auth);
        if ("SUPER_ADMIN".equalsIgnoreCase(body.get("role"))) {
            throw new ForbiddenException("A school account cannot be promoted to platform administrator");
        }

        UserDto updated = authService.updateUser(
                userId,
                body.get("role"),
                schoolId,
                body.get("phone"),
                body.containsKey("active") ? Boolean.valueOf(body.get("active")) : null,
                body.get("password"),
                body.containsKey("notifyEmail") ? Boolean.valueOf(body.get("notifyEmail")) : null,
                body.containsKey("notifySms") ? Boolean.valueOf(body.get("notifySms")) : null
        );
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String schoolId, @PathVariable String userId, Authentication auth) {
        RoleGuard.requireSchoolAccountManager(auth);
        assertTargetBelongsToSchool(schoolId, userId, auth);
        authService.deleteUserPermanently(auth.getName(), userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    private void assertTargetBelongsToSchool(String schoolId, String userId, Authentication auth) {
        if (RoleGuard.isSuperAdmin(auth)) return;
        UserDto target = authService.getMe(userId);
        if (!schoolId.equals(target.getSchoolId())) {
            throw new ForbiddenException("You cannot manage an account from another school");
        }
    }
}
