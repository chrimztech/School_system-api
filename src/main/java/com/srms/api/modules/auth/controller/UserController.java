package com.srms.api.modules.auth.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schools/{schoolId}/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> list(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getUsersBySchool(schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> create(
            @PathVariable String schoolId,
            @RequestBody Map<String, String> body) {

        AppUser user = new AppUser();
        user.setEmail(body.get("email"));
        user.setName(body.get("name"));
        user.setSchoolId(schoolId);
        user.setActive(true);
        user.setRole(authService.parseRole(body.getOrDefault("role", "SCHOOL_ADMIN")));

        if (body.containsKey("phone")) user.setPhone(body.get("phone"));

        UserDto created = authService.createUser(user, body.getOrDefault("password", "password123"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> update(
            @PathVariable String schoolId,
            @PathVariable String userId,
            @RequestBody Map<String, String> body) {

        UserDto updated = authService.updateUser(
                userId,
                body.get("role"),
                schoolId,
                body.get("phone"),
                body.containsKey("active") ? Boolean.valueOf(body.get("active")) : null,
                body.get("password")
        );
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
    }
}