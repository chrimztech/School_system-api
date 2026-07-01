package com.srms.api.modules.auth.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.modules.auth.dto.AuthResponse;
import com.srms.api.modules.auth.dto.LoginRequest;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        if (!user.isActive()) throw new BusinessException("Account is deactivated");
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getSchoolId());
        return AuthResponse.builder()
                .token(token).id(user.getId()).name(user.getName())
                .email(user.getEmail()).role(user.getRole().name())
                .schoolId(user.getSchoolId()).initials(user.getInitials())
                .build();
    }

    public UserDto getMe(String userId) {
        return toDto(findUserEntity(userId));
    }

    public List<UserDto> getUsersBySchool(String schoolId) {
        return userRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .sorted(Comparator.comparing(AppUser::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(AppUser::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    public UserDto createUser(AppUser user, String rawPassword) {
        return toDto(createUserEntity(user, rawPassword));
    }

    public UserDto updateUser(String userId, String roleValue, String schoolId, String phone, Boolean active) {
        return updateUser(userId, roleValue, schoolId, phone, active, null);
    }

    public UserDto updateUser(String userId, String roleValue, String schoolId, String phone, Boolean active, String rawPassword) {
        return updateUser(userId, roleValue, schoolId, phone, active, rawPassword, null, null);
    }

    public UserDto updateUser(String userId, String roleValue, String schoolId, String phone, Boolean active, String rawPassword, Boolean notifyEmail, Boolean notifySms) {
        AppUser user = findUserEntity(userId);
        AppUser.UserRole nextRole = roleValue == null || roleValue.isBlank() ? user.getRole() : parseRole(roleValue);
        String resolvedSchoolId = nextRole == AppUser.UserRole.SUPER_ADMIN
                ? null
                : (schoolId != null && !schoolId.isBlank() ? schoolId : user.getSchoolId());

        if (nextRole != AppUser.UserRole.SUPER_ADMIN && (resolvedSchoolId == null || resolvedSchoolId.isBlank())) {
            throw new BusinessException("A school is required for non-system users");
        }

        user.setRole(nextRole);
        user.setSchoolId(resolvedSchoolId);
        if (phone != null) user.setPhone(phone);
        if (active != null) user.setActive(active);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        if (notifyEmail != null) user.setNotifyEmail(notifyEmail);
        if (notifySms != null) user.setNotifySms(notifySms);

        return toDto(userRepository.save(user));
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        AppUser user = findUserEntity(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("New password must be at least 8 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deactivateUser(String userId) {
        AppUser user = findUserEntity(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    public AppUser.UserRole parseRole(String value) {
        return switch ((value == null ? "" : value).trim().toUpperCase().replace(" ", "_")) {
            case "SUPER_ADMIN" -> AppUser.UserRole.SUPER_ADMIN;
            case "TEACHER" -> AppUser.UserRole.TEACHER;
            case "HOD", "HEAD_OF_DEPARTMENT" -> AppUser.UserRole.HOD;
            case "FINANCE", "FINANCE_OFFICER" -> AppUser.UserRole.FINANCE;
            case "PARENT" -> AppUser.UserRole.PARENT;
            case "PRINCIPAL", "HEAD_MASTER", "HEADMASTER" -> AppUser.UserRole.PRINCIPAL;
            case "DEPUTY_HEAD", "DEPUTY_HEADTEACHER" -> AppUser.UserRole.DEPUTY_HEAD;
            case "CAREER_GUIDANCE", "CAREER_GUIDANCE_TEACHER" -> AppUser.UserRole.CAREER_GUIDANCE;
            default -> AppUser.UserRole.SCHOOL_ADMIN;
        };
    }

    public UserDto toDto(AppUser user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .initials(user.getInitials())
                .schoolId(user.getSchoolId())
                .phone(user.getPhone())
                .active(user.isActive())
                .notifyEmail(user.isNotifyEmail())
                .notifySms(user.isNotifySms())
                .build();
    }

    private AppUser createUserEntity(AppUser user, String rawPassword) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("Email is required");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new BusinessException("Name is required");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BusinessException("A user with this email already exists");
        }

        AppUser.UserRole role = user.getRole() == null ? AppUser.UserRole.SCHOOL_ADMIN : user.getRole();
        user.setRole(role);

        if (role == AppUser.UserRole.SUPER_ADMIN) {
            user.setSchoolId(null);
        } else if (user.getSchoolId() == null || user.getSchoolId().isBlank()) {
            throw new BusinessException("A school is required for non-system users");
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank() ? "password123" : rawPassword));
        if (user.getInitials() == null && user.getName() != null) {
            String[] parts = user.getName().trim().split("\\s+");
            user.setInitials(parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                    : String.valueOf(parts[0].charAt(0)));
        }
        return userRepository.save(user);
    }

    private AppUser findUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }
}