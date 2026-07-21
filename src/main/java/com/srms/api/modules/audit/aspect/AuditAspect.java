package com.srms.api.modules.audit.aspect;

import com.srms.api.common.BaseEntity;
import com.srms.api.modules.audit.entity.AuditEvent;
import com.srms.api.modules.audit.repository.AuditEventRepository;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes an {@link AuditEvent} for every mutation reachable through a *Service bean under
 * com.srms.api.modules — the trail this app previously had a table and endpoints for, but
 * nothing ever actually wrote to. Read-only methods are skipped by naming convention (see
 * READ_ONLY_PREFIXES) rather than an explicit allow-list, since every module follows the same
 * find/get/list-for-reads, create/update/delete-for-writes convention already.
 *
 * Spring AOP proxies only intercept calls that arrive from *outside* the bean, so internal
 * self-invocation (a service method calling its own private/public helper) is never
 * double-logged — that's a feature here, not a gap.
 */
@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private static final List<String> READ_ONLY_PREFIXES = List.of(
            "find", "get", "list", "search", "count", "compute", "is", "has", "to", "parse",
            "resolve", "effective", "assert", "belongs", "stats", "validate", "check", "build",
            "read", "load", "fetch"
    );
    // Handled explicitly elsewhere (no authenticated principal yet at call time) or not
    // meaningful as an audited "mutation".
    private static final List<String> EXPLICITLY_EXCLUDED = List.of("login", "toDto");

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public AuditAspect(AuditEventRepository auditEventRepository, UserRepository userRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    @Around("execution(public * com.srms.api.modules..service..*Service.*(..)) "
            + "&& !within(com.srms.api.modules.audit..*)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        if (isReadOnly(methodName)) return pjp.proceed();

        try {
            Object result = pjp.proceed();
            recordSafely(pjp, methodName, result, null);
            return result;
        } catch (Throwable ex) {
            recordSafely(pjp, methodName, null, ex);
            throw ex;
        }
    }

    private boolean isReadOnly(String methodName) {
        if (EXPLICITLY_EXCLUDED.contains(methodName)) return true;
        String lower = methodName.toLowerCase(Locale.ROOT);
        for (String prefix : READ_ONLY_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }

    private void recordSafely(ProceedingJoinPoint pjp, String methodName, Object result, Throwable error) {
        try {
            recordEvent(pjp, methodName, result, error);
        } catch (Exception e) {
            // Audit logging must never take down the actual request.
            log.warn("Failed to write audit event for {}", methodName, e);
        }
    }

    private void recordEvent(ProceedingJoinPoint pjp, String methodName, Object result, Throwable error) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String schoolId = resolveSchoolId(pjp, auth);
        if (schoolId == null || schoolId.isBlank()) return; // can't attribute to a tenant — skip rather than guess

        String actor = "System";
        String role = "SYSTEM";
        if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            AppUser user = userRepository.findById(auth.getName()).orElse(null);
            if (user != null) {
                actor = user.getName() + " <" + user.getEmail() + ">";
                role = user.getRole().name();
            } else {
                actor = auth.getName();
            }
        }

        String className = pjp.getTarget().getClass().getSimpleName();
        String action = className + "." + methodName;
        String target = error != null
                ? summarize(lastArg(pjp))
                : summarize(result != null ? result : lastArg(pjp));

        AuditEvent event = new AuditEvent();
        event.setSchoolId(schoolId);
        event.setActor(actor);
        event.setRole(role);
        event.setAction(action);
        event.setTarget(error != null ? target + " — failed: " + safeMessage(error) : target);
        event.setSeverity(error != null ? "warning" : "success");
        auditEventRepository.save(event);
    }

    private String resolveSchoolId(ProceedingJoinPoint pjp, Authentication auth) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length && i < args.length; i++) {
                if ("schoolId".equals(names[i]) && args[i] instanceof String s && !s.isBlank()) return s;
            }
        }
        if (auth != null && auth.getCredentials() != null) {
            String cred = auth.getCredentials().toString();
            if (!cred.isBlank() && !"null".equals(cred)) return cred;
        }
        return null;
    }

    private Object lastArg(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        return args.length > 0 ? args[args.length - 1] : null;
    }

    private String summarize(Object obj) {
        if (obj == null) return "";
        try {
            if (obj instanceof BaseEntity entity) {
                return obj.getClass().getSimpleName() + ":" + entity.getId();
            }
            if (obj instanceof Collection<?> collection) {
                return obj.getClass().getSimpleName() + "[" + collection.size() + "]";
            }
            if (obj instanceof Map<?, ?> map) {
                return "Map[" + map.size() + "]";
            }
            String s = String.valueOf(obj);
            return s.length() > 300 ? s.substring(0, 300) + "…" : s;
        } catch (Exception e) {
            return obj.getClass().getSimpleName();
        }
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null) return error.getClass().getSimpleName();
        return message.length() > 200 ? message.substring(0, 200) + "…" : message;
    }
}
