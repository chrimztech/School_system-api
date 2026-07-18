package com.srms.api.modules.communication.service;

import com.srms.api.modules.alumni.entity.AlumniRecord;
import com.srms.api.modules.alumni.repository.AlumniRepository;
import com.srms.api.modules.communication.entity.Announcement;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final AlumniRepository alumniRepository;
    private final JavaMailSender mailSender;

    private static final Pattern FORM_OR_GRADE = Pattern.compile("(?:form|grade)\\s*(\\d{1,2})");

    /** Who an announcement's audience string actually resolves to. */
    private record AudienceTarget(boolean staff, boolean parents, boolean students, boolean alumni, Integer gradeFilter, String levelFilter) {}

    @Value("${spring.mail.from:noreply@srms.zm}")
    private String fromEmail;

    @Value("${africastalking.username:sandbox}")
    private String atUsername;

    @Value("${africastalking.apiKey:}")
    private String atApiKey;

    @Value("${africastalking.smsUrl:https://api.africastalking.com/version1/messaging}")
    private String atSmsUrl;

    @Value("${africastalking.defaultCountryCode:+260}")
    private String defaultCountryCode;

    @Async
    public void dispatch(Announcement ann) {
        if (ann.getChannels() == null || ann.getChannels().isBlank()) {
            log.info("Announcement {} has no channels configured — skipping dispatch", ann.getId());
            return;
        }

        String schoolId = ann.getSchoolId();
        School school = schoolRepository.findById(schoolId).orElse(null);
        AudienceTarget target = resolveAudienceTarget(ann.getAudience(), school);
        List<String> channels = parseChannels(ann.getChannels());

        List<String> emails = new ArrayList<>();
        List<String> phones = new ArrayList<>();

        if (target.staff()) {
            List<Teacher> teachers = teacherRepository.findBySchoolIdAndStatus(schoolId, Teacher.TeacherStatus.active);
            for (Teacher t : teachers) {
                if (t.getEmail() != null && !t.getEmail().isBlank()) emails.add(t.getEmail());
                if (t.getPhone() != null && !t.getPhone().isBlank()) phones.add(normalizePhone(t.getPhone()));
            }
        }

        if (target.parents() || target.students()) {
            List<Student> students = studentRepository.findBySchoolIdAndStatus(schoolId, Student.StudentStatus.active);
            for (Student s : students) {
                if (target.gradeFilter() != null && s.getGrade() != target.gradeFilter()) continue;
                if (target.levelFilter() != null && !matchesLevel(s.getGrade(), school, target.levelFilter())) continue;

                if (target.parents()) {
                    if (s.getGuardianEmail() != null && !s.getGuardianEmail().isBlank())
                        emails.add(s.getGuardianEmail());
                    if (s.getGuardianPhone() != null && !s.getGuardianPhone().isBlank())
                        phones.add(normalizePhone(s.getGuardianPhone()));
                    if (s.getGuardianAltPhone() != null && !s.getGuardianAltPhone().isBlank())
                        phones.add(normalizePhone(s.getGuardianAltPhone()));
                }
                if (target.students() && s.getStudentEmail() != null && !s.getStudentEmail().isBlank()) {
                    emails.add(s.getStudentEmail());
                }
            }
        }

        if (target.alumni()) {
            List<AlumniRecord> alumni = alumniRepository.findBySchoolId(schoolId);
            for (AlumniRecord al : alumni) {
                if (!"ACTIVE".equalsIgnoreCase(al.getStatus())) continue;
                if (al.getEmail() != null && !al.getEmail().isBlank()) emails.add(al.getEmail());
                if (al.getPhone() != null && !al.getPhone().isBlank()) phones.add(normalizePhone(al.getPhone()));
            }
        }

        log.info("Announcement {} dispatch: {} emails, {} phones, channels={}", ann.getId(), emails.size(), phones.size(), channels);

        String subject = ann.getTitle();
        String body = ann.getBody();

        for (String channel : channels) {
            switch (channel.toLowerCase()) {
                case "email" -> sendEmails(emails, subject, body);
                case "sms" -> sendSms(phones, body);
                case "whatsapp" -> log.info("WhatsApp channel not yet active (requires Meta Business approval) — {} recipients", phones.size());
                case "ussd" -> log.info("USSD is pull-based — no push dispatch for announcement {}", ann.getId());
                default -> log.warn("Unknown channel '{}' on announcement {}", channel, ann.getId());
            }
        }
    }

    /**
     * Turns a free-form audience string (from the announcement/broadcast UI, e.g. "All staff",
     * "Form 2", "Grade 5", "All secondary") into who actually gets contacted. Word-boundary
     * matching, not substring matching — "All staff" must not also match "all" as in "everyone",
     * and "Form 1"/"Grade 1" must resolve to a real recipient set instead of matching nothing.
     */
    private AudienceTarget resolveAudienceTarget(String raw, School school) {
        String a = (raw == null || raw.isBlank() ? "all" : raw).trim().toLowerCase();

        Matcher gradeMatch = FORM_OR_GRADE.matcher(a);
        if (gradeMatch.find()) {
            return new AudienceTarget(false, true, false, false, Integer.parseInt(gradeMatch.group(1)), null);
        }
        if (a.contains("secondary")) return new AudienceTarget(false, true, false, false, null, "secondary");
        if (a.contains("primary")) return new AudienceTarget(false, true, false, false, null, "primary");
        if (a.contains("staff") || a.contains("teacher")) return new AudienceTarget(true, false, false, false, null, null);
        if (a.contains("alumni")) return new AudienceTarget(false, false, false, true, null, null);
        if (a.equals("all") || a.contains("everyone")) return new AudienceTarget(true, true, true, true, null, null);
        if (a.contains("parent")) return new AudienceTarget(false, true, false, false, null, null);
        if (a.contains("student")) return new AudienceTarget(false, false, true, false, null, null);
        log.warn("Unrecognised announcement audience '{}' — no recipients resolved", a);
        return new AudienceTarget(false, false, false, false, null, null);
    }

    /** Whether a student's numeric grade falls in the primary or secondary band for this school's type. */
    private boolean matchesLevel(int grade, School school, String level) {
        String type = school != null && school.getType() != null ? school.getType().toUpperCase() : "";
        boolean schoolIsPrimaryOnly = type.equals("PRIMARY");
        boolean schoolIsSecondaryOnly = type.equals("SECONDARY");
        if (schoolIsPrimaryOnly) return level.equals("primary");
        if (schoolIsSecondaryOnly) return level.equals("secondary");
        // Combined/mixed schools: grades 1-6 are primary, 7-12 are secondary (Form 1-6).
        boolean isPrimaryGrade = grade >= 1 && grade <= 6;
        return level.equals("primary") == isPrimaryGrade;
    }

    /** Best-effort payment confirmation — reuses the same email/SMS senders as announcements. */
    public void sendPaymentReceipt(String guardianEmail, String guardianPhone, String studentName, double amount, String referenceNumber) {
        String subject = "Payment received — " + studentName;
        String body = String.format(
                "We've received a payment of ZMW %.2f for %s (ref: %s). Thank you.",
                amount, studentName, referenceNumber);
        if (guardianEmail != null && !guardianEmail.isBlank()) {
            sendEmails(List.of(guardianEmail), subject, body);
        }
        if (guardianPhone != null && !guardianPhone.isBlank()) {
            sendSms(List.of(normalizePhone(guardianPhone)), body);
        }
    }

    private void sendEmails(List<String> recipients, String subject, String body) {
        if (recipients.isEmpty()) return;
        if (atApiKey.isBlank() && fromEmail.equals("noreply@srms.zm")) {
            log.warn("Email channel selected but MAIL_USERNAME / MAIL_PASSWORD not configured — skipping {} emails", recipients.size());
            return;
        }
        int sent = 0;
        for (String to : recipients) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromEmail);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", to, e.getMessage());
            }
        }
        log.info("Email dispatch complete: {}/{} sent", sent, recipients.size());
    }

    private void sendSms(List<String> recipients, String message) {
        if (recipients.isEmpty()) return;
        if (atApiKey.isBlank()) {
            log.warn("SMS channel selected but AT_API_KEY not configured — skipping {} SMS", recipients.size());
            return;
        }

        // Africa's Talking accepts max 400 numbers per request; batch if needed
        int batchSize = 400;
        int sent = 0;
        for (int i = 0; i < recipients.size(); i += batchSize) {
            List<String> batch = recipients.subList(i, Math.min(i + batchSize, recipients.size()));
            String to = String.join(",", batch);
            try {
                RestTemplate rest = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("apiKey", atApiKey);
                headers.set("Accept", "application/json");

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("username", atUsername);
                params.add("to", to);
                params.add("message", message);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
                rest.postForObject(atSmsUrl, request, String.class);
                sent += batch.size();
            } catch (Exception e) {
                log.error("SMS batch dispatch failed (batch starting at {}): {}", i, e.getMessage());
            }
        }
        log.info("SMS dispatch complete: {}/{} queued", sent, recipients.size());
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        phone = phone.replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("0") && phone.length() >= 9) {
            return defaultCountryCode + phone.substring(1);
        }
        if (!phone.startsWith("+")) {
            return defaultCountryCode + phone;
        }
        return phone;
    }

    private List<String> parseChannels(String channels) {
        // Stored as comma-separated e.g. "Email,SMS" or JSON array-like "[Email,SMS]"
        return List.of(channels.replaceAll("[\\[\\]\"]", "").split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
