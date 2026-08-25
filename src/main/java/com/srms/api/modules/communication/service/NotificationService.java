package com.srms.api.modules.communication.service;

import com.srms.api.common.PhoneUtils;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    private final ZamtelSmsClient smsClient;

    private static final Pattern FORM_OR_GRADE = Pattern.compile("(?:form|grade)\\s*(\\d{1,2})");
    /** Every contact is a literal URL path segment (see ZamtelSmsClient.send), not a request
     *  body field, so this is deliberately small to stay well under common server/proxy URL
     *  length limits even with a long message and a long sender id. */
    private static final int SMS_BATCH_SIZE = 50;

    /** Who an announcement's audience string actually resolves to. */
    private record AudienceTarget(boolean staff, boolean parents, boolean students, boolean alumni, Integer gradeFilter, String levelFilter) {}

    @Value("${spring.mail.from:noreply@srms.zm}")
    private String fromEmail;

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
                if (t.getPhone() != null && !t.getPhone().isBlank()) phones.add(PhoneUtils.normalize(t.getPhone()));
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
                        phones.add(PhoneUtils.normalize(s.getGuardianPhone()));
                    if (s.getGuardianAltPhone() != null && !s.getGuardianAltPhone().isBlank())
                        phones.add(PhoneUtils.normalize(s.getGuardianAltPhone()));
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
                if (al.getPhone() != null && !al.getPhone().isBlank()) phones.add(PhoneUtils.normalize(al.getPhone()));
            }
        }

        log.info("Announcement {} dispatch: {} emails, {} phones, channels={}", ann.getId(), emails.size(), phones.size(), channels);

        String subject = ann.getTitle();
        String body = ann.getBody();

        for (String channel : channels) {
            switch (channel.toLowerCase()) {
                case "email" -> sendEmails(emails, subject, body);
                case "sms" -> {
                    String senderId = school != null ? school.getSmsSenderId() : null;
                    sendSms(phones, prefixSchoolIfSharedSender(body, school, senderId), senderId);
                }
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
    public void sendPaymentReceipt(String schoolId, String guardianEmail, String guardianPhone, String studentName, double amount, String referenceNumber) {
        String subject = "Payment received — " + studentName;
        String body = String.format(
                "We've received a payment of ZMW %.2f for %s (ref: %s). Thank you.",
                amount, studentName, referenceNumber);
        if (guardianEmail != null && !guardianEmail.isBlank()) {
            sendEmails(List.of(guardianEmail), subject, body);
        }
        if (guardianPhone != null && !guardianPhone.isBlank()) {
            School school = schoolRepository.findById(schoolId).orElse(null);
            String senderId = school != null ? school.getSmsSenderId() : null;
            sendSms(List.of(PhoneUtils.normalize(guardianPhone)), prefixSchoolIfSharedSender(body, school, senderId), senderId);
        }
    }

    /**
     * Every school currently shares the one Zamtel-approved sender ID ("DCL") until each school's
     * own short code gets individually registered and approved — so the sender line alone can't
     * tell a recipient which school messaged them. Prefix the school name in that case; skip it
     * once a school has its own approved sender ID, since the sender line already identifies it.
     */
    private String prefixSchoolIfSharedSender(String body, School school, String senderId) {
        if (senderId != null && !senderId.isBlank()) return body;
        if (school == null || school.getName() == null || school.getName().isBlank()) return body;
        return "[" + school.getName() + "] " + body;
    }

    private void sendEmails(List<String> recipients, String subject, String body) {
        if (recipients.isEmpty()) return;
        if (fromEmail.equals("noreply@srms.zm")) {
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

    private void sendSms(List<String> recipients, String message, String senderId) {
        if (recipients.isEmpty()) return;
        if (!smsClient.isConfigured()) {
            log.warn("SMS channel selected but zamtel.bulksms.api-key not configured — skipping {} SMS", recipients.size());
            return;
        }

        int sent = 0;
        for (int i = 0; i < recipients.size(); i += SMS_BATCH_SIZE) {
            List<String> batch = recipients.subList(i, Math.min(i + SMS_BATCH_SIZE, recipients.size()));
            if (smsClient.send(batch, message, senderId)) sent += batch.size();
        }
        log.info("SMS dispatch complete: {}/{} queued via Zamtel BulkSMS", sent, recipients.size());
    }

    /** Remaining Zamtel SMS credit — surfaced to admins before a bulk broadcast. -1 if unreachable/not configured. */
    public long getSmsBalance() {
        return smsClient.getBalance();
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
