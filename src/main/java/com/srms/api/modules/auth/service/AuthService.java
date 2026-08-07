package com.srms.api.modules.auth.service;

import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.repository.AuditEventRepository;
import com.srms.api.modules.auth.dto.AuthResponse;
import com.srms.api.modules.auth.dto.LoginRequest;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import com.srms.api.security.JwtTokenProvider;
import com.srms.api.security.tenant.TenantResolution;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final AuditEventRepository auditEventRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // Roles whose assessment/verification workflows resolve identity through the Teacher
    // (HR staff) table by email — see AcademicService.findAssignmentsByTeacherEmail and
    // AssessmentService's HOD department-verification checks. A login account for one of
    // these roles is useless for those workflows without a matching Teacher row, so we
    // keep the two in sync at creation time instead of relying on someone remembering to
    // add the staff profile separately.
    private static final Set<AppUser.UserRole> ROLES_REQUIRING_TEACHER_PROFILE =
            EnumSet.of(AppUser.UserRole.TEACHER, AppUser.UserRole.HOD);

    public AuthResponse login(LoginRequest request, TenantResolution tenant) {
        AppUser user = findUserByIdentifier(request.getIdentifier());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginAttempt(user, "warning", "Failed sign-in attempt");
            throw new BusinessException("Invalid email/phone or password");
        }
        if (!user.isActive()) {
            recordLoginAttempt(user, "warning", "Sign-in blocked — account deactivated");
            throw new BusinessException("Account is deactivated");
        }
        if (tenant.scope() == TenantResolution.Scope.PLATFORM
                && user.getRole() != AppUser.UserRole.SUPER_ADMIN) {
            recordLoginAttempt(user, "warning", "Sign-in blocked on platform domain");
            throw new ForbiddenException("School accounts must use their school subdomain");
        }
        if (tenant.isTenant() && (user.getRole() == AppUser.UserRole.SUPER_ADMIN
                || !tenant.schoolId().equals(user.getSchoolId()))) {
            recordLoginAttempt(user, "warning", "Cross-school sign-in blocked");
            throw new ForbiddenException("This account does not belong to this school");
        }
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getSchoolId());
        recordLoginAttempt(user, "success", "Signed in");
        return AuthResponse.builder()
                .token(token).id(user.getId()).name(user.getName())
                .email(user.getEmail()).phone(user.getPhone()).role(user.getRole().name())
                .schoolId(user.getSchoolId()).initials(user.getInitials())
                .build();
    }

    /**
     * Accepts either an email or a phone number — some accounts (e.g. parents without email)
     * only have a phone on file. An "@" is treated as an email lookup; otherwise the identifier
     * is normalized (spaces/dashes stripped) and matched against the stored phone.
     */
    private AppUser findUserByIdentifier(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.contains("@")) {
            return userRepository.findByEmail(trimmed).orElse(null);
        }
        List<AppUser> matches = userRepository.findByPhone(normalizePhone(trimmed));
        if (matches.size() > 1) {
            throw new BusinessException(
                    "Multiple accounts are linked to this phone number. Please sign in with your email instead, or contact your school office.");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String normalizePhone(String phone) {
        return PhoneUtils.normalize(phone);
    }

    /**
     * The AuditAspect can't see this call — there's no authenticated principal yet when a
     * login is being attempted — so login attempts are logged explicitly here instead.
     * Attempts against an unknown email, or a SUPER_ADMIN account (schoolId is always null
     * for those), aren't recorded: AuditEvent.schoolId is NOT NULL and there is no tenant
     * to attribute the event to.
     */
    private void recordLoginAttempt(AppUser user, String severity, String action) {
        if (user == null || user.getSchoolId() == null || user.getSchoolId().isBlank()) return;
        AuditEvent event = new AuditEvent();
        event.setSchoolId(user.getSchoolId());
        String contact = user.getEmail() != null ? user.getEmail() : user.getPhone();
        event.setActor(user.getName() + " <" + contact + ">");
        event.setRole(user.getRole().name());
        event.setAction("AuthService.login");
        event.setTarget(action);
        event.setSeverity(severity);
        auditEventRepository.save(event);
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
        if (phone != null) user.setPhone(phone.isBlank() ? null : normalizePhone(phone));
        if (active != null) user.setActive(active);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        if (notifyEmail != null) user.setNotifyEmail(notifyEmail);
        if (notifySms != null) user.setNotifySms(notifySms);

        AppUser saved = userRepository.save(user);
        ensureTeacherProfile(saved);
        return toDto(saved);
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
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();
        if (!hasEmail && !hasPhone) {
            throw new BusinessException("Email or phone is required");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new BusinessException("Name is required");
        }
        if (hasEmail && userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BusinessException("A user with this email already exists");
        }
        if (hasPhone) {
            user.setPhone(normalizePhone(user.getPhone()));
            if (!userRepository.findByPhone(user.getPhone()).isEmpty()) {
                throw new BusinessException("A user with this phone number already exists");
            }
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
        AppUser saved = userRepository.save(user);
        ensureTeacherProfile(saved);
        return saved;
    }

    /**
     * Guarantees a login account with a teaching role has a matching Teacher (HR staff)
     * row for the same email in the same school. Without this, class/subject assignment
     * lookups and HOD department-verification (both keyed on Teacher.email) silently
     * return nothing for an otherwise valid, active login — see AcademicService and
     * AssessmentService's teacherRepository.findByEmailIgnoreCaseAndSchoolId usages.
     */
    private void ensureTeacherProfile(AppUser user) {
        if (!ROLES_REQUIRING_TEACHER_PROFILE.contains(user.getRole())) return;
        if (user.getSchoolId() == null || user.getSchoolId().isBlank()) return;
        if (teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), user.getSchoolId()).isPresent()) return;

        String[] parts = user.getName().trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : parts[0];
        long count = teacherRepository.countBySchoolIdAndStatus(user.getSchoolId(), Teacher.TeacherStatus.active);

        Teacher teacher = new Teacher();
        teacher.setSchoolId(user.getSchoolId());
        teacher.setStaffNumber("STF-" + user.getSchoolId().toUpperCase() + "-" + String.format("%03d", count + 1));
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(user.getEmail());
        teacher.setPhone(user.getPhone());
        teacher.setStatus(Teacher.TeacherStatus.active);
        teacherRepository.save(teacher);
    }

    private AppUser findUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }
}
