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

import java.security.SecureRandom;
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
                .mustChangePassword(user.isMustChangePassword())
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

    private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();
    // Excludes visually ambiguous characters (0/O, 1/l/I) — this password is usually read aloud
    // or retyped from a phone screen by whoever the admin shares it with.
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(TEMP_PASSWORD_ALPHABET.charAt(TEMP_PASSWORD_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    public UserDto createUser(AppUser user, String rawPassword) {
        // A blank password used to silently fall back to the literal string "password123" for
        // every admin-created account — a guessable standing credential for anyone who knows the
        // new user's email, and the admin had no idea it had happened. Generating a random one
        // per account and handing it back here (once, in this response only) closes that hole
        // without losing the "leave it blank" convenience.
        boolean generated = rawPassword == null || rawPassword.isBlank();
        String effectivePassword = generated ? generateTemporaryPassword() : rawPassword;
        UserDto dto = toDto(createUserEntity(user, effectivePassword));
        if (generated) dto.setTemporaryPassword(effectivePassword);
        return dto;
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
            user.setMustChangePassword(true);
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
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public void deactivateUser(String userId) {
        AppUser user = findUserEntity(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * A real, permanent delete — distinct from deactivateUser(), which only flips the active
     * flag. No other table has a real foreign key onto app_users (every cross-reference in this
     * app, e.g. audit_events.actor, is a loose string column, not an FK constraint), so this is
     * safe at the database level; any historical record naming this user simply keeps showing
     * their old name/id rather than resolving to a live account.
     */
    public void deleteUserPermanently(String requestingUserId, String userId) {
        if (requestingUserId != null && requestingUserId.equals(userId)) {
            throw new BusinessException("You cannot delete your own account");
        }
        AppUser user = findUserEntity(userId);
        userRepository.delete(user);
    }

    public AppUser.UserRole parseRole(String value) {
        String normalized = (value == null ? "" : value).trim().toUpperCase().replace(" ", "_");
        return switch (normalized) {
            case "SUPER_ADMIN" -> AppUser.UserRole.SUPER_ADMIN;
            case "SCHOOL_ADMIN" -> AppUser.UserRole.SCHOOL_ADMIN;
            case "TEACHER" -> AppUser.UserRole.TEACHER;
            case "HOD", "HEAD_OF_DEPARTMENT" -> AppUser.UserRole.HOD;
            case "FINANCE", "FINANCE_OFFICER" -> AppUser.UserRole.FINANCE;
            case "PARENT" -> AppUser.UserRole.PARENT;
            case "PRINCIPAL", "HEAD_MASTER", "HEADMASTER" -> AppUser.UserRole.PRINCIPAL;
            case "DEPUTY_HEAD", "DEPUTY_HEADTEACHER" -> AppUser.UserRole.DEPUTY_HEAD;
            case "CAREER_GUIDANCE", "CAREER_GUIDANCE_TEACHER" -> AppUser.UserRole.CAREER_GUIDANCE;
            // An unrecognized role must never silently become SCHOOL_ADMIN — one of the most
            // privileged roles in the system. A typo, a new frontend role added without a
            // matching case here, or a malformed direct API call should reject the request, not
            // quietly grant elevated access.
            default -> throw new BusinessException("Unrecognized role: \"" + value + "\"");
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
                .mustChangePassword(user.isMustChangePassword())
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

        // rawPassword is always concrete here — createUser() above resolves a blank/omitted
        // password to a freshly generated one before calling this.
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        // A newly provisioned account's password is always known to whoever created it (the
        // admin) — force a change on first sign-in so that knowledge doesn't stay a standing
        // credential.
        user.setMustChangePassword(true);
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
