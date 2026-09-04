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
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

        String subject = (isUrgent(ann.getPriority()) ? ann.getPriority().toUpperCase() + ": " : "") + ann.getTitle();
        String body = ann.getBody();

        for (String channel : channels) {
            switch (channel.toLowerCase()) {
                case "email" -> sendEmails(emails, subject,
                        buildEmailHtml(school, ann.getTitle(), body, ann.getPriority()),
                        buildEmailPlainText(school, ann.getTitle(), body, ann.getPriority()),
                        school);
                case "sms" -> {
                    String senderId = school != null ? school.getSmsSenderId() : null;
                    String smsBody = withPriorityPrefix(prefixSchoolIfSharedSender(body, school, senderId), ann.getPriority());
                    sendSms(phones, smsBody, senderId);
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
        School school = schoolRepository.findById(schoolId).orElse(null);
        String heading = "Payment received";
        String subject = heading + " — " + studentName;
        String body = String.format(
                "We've received a payment of ZMW %.2f for %s (ref: %s). Thank you.",
                amount, studentName, referenceNumber);
        if (guardianEmail != null && !guardianEmail.isBlank()) {
            sendEmails(List.of(guardianEmail), subject,
                    buildEmailHtml(school, heading, body, null),
                    buildEmailPlainText(school, heading, body, null),
                    school);
        }
        if (guardianPhone != null && !guardianPhone.isBlank()) {
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

    private boolean isUrgent(String priority) {
        return priority != null && (priority.equalsIgnoreCase("Urgent") || priority.equalsIgnoreCase("Emergency"));
    }

    /** Leads with the severity word so it survives an SMS preview's character truncation, ahead
     *  of even the school-name prefix. */
    private String withPriorityPrefix(String body, String priority) {
        return isUrgent(priority) ? priority.toUpperCase() + ": " + body : body;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Letterhead-style HTML email: school name/motto banner in the school's own brand colour,
     * an urgency banner for Urgent/Emergency announcements, the message body, and a footer with
     * a way to reach the school back. Deliberately doesn't embed the school's logo — several
     * major webmail clients (Gmail included) strip inline data-URI images from received HTML
     * mail for security, so an embedded logo would just as often render as a broken image as a
     * real one; the text banner in the brand colour is the reliable way to get the same effect.
     */
    private String buildEmailHtml(School school, String heading, String bodyText, String priority) {
        String schoolName = school != null && school.getName() != null && !school.getName().isBlank() ? school.getName() : "School";
        String accent = school != null && school.getPrimaryColor() != null && !school.getPrimaryColor().isBlank() ? school.getPrimaryColor() : "#1e3a5f";
        String motto = school != null ? school.getMotto() : null;
        String contact = contactLine(school);
        String escapedBody = escapeHtml(bodyText).replace("\n", "<br/>");

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><body style=\"margin:0;padding:0;background:#f4f5f7;font-family:'Segoe UI',Arial,sans-serif;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f5f7;padding:24px 0;\"><tr><td align=\"center\">");
        sb.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:8px;overflow:hidden;max-width:600px;\">");
        sb.append("<tr><td style=\"background:").append(accent).append(";padding:20px 28px;\">");
        sb.append("<div style=\"color:#ffffff;font-size:18px;font-weight:600;\">").append(escapeHtml(schoolName)).append("</div>");
        if (motto != null && !motto.isBlank()) {
            sb.append("<div style=\"color:#ffffff;opacity:0.85;font-size:12px;margin-top:2px;\">").append(escapeHtml(motto)).append("</div>");
        }
        sb.append("</td></tr>");
        if (isUrgent(priority)) {
            sb.append("<tr><td style=\"background:#fef2f2;padding:10px 28px;border-bottom:1px solid #fecaca;\">");
            sb.append("<span style=\"color:#b91c1c;font-size:12px;font-weight:700;letter-spacing:.05em;\">").append(escapeHtml(priority.toUpperCase())).append(" NOTICE</span>");
            sb.append("</td></tr>");
        }
        sb.append("<tr><td style=\"padding:28px;\">");
        sb.append("<h2 style=\"margin:0 0 12px;color:#111827;font-size:18px;\">").append(escapeHtml(heading)).append("</h2>");
        sb.append("<div style=\"color:#374151;font-size:14px;line-height:1.6;\">").append(escapedBody).append("</div>");
        sb.append("</td></tr>");
        sb.append("<tr><td style=\"padding:16px 28px;border-top:1px solid #e5e7eb;\">");
        sb.append("<div style=\"color:#9ca3af;font-size:12px;\">").append(escapeHtml(schoolName));
        if (!contact.isBlank()) sb.append(" &middot; ").append(escapeHtml(contact));
        sb.append("</div></td></tr>");
        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }

    /** Plain-text alternative for mail clients that don't render HTML. */
    private String buildEmailPlainText(School school, String heading, String bodyText, String priority) {
        String schoolName = school != null && school.getName() != null && !school.getName().isBlank() ? school.getName() : "School";
        String contact = contactLine(school);
        StringBuilder sb = new StringBuilder();
        sb.append(schoolName.toUpperCase()).append("\n");
        if (isUrgent(priority)) sb.append(priority.toUpperCase()).append(" NOTICE\n");
        sb.append("\n").append(heading).append("\n\n");
        sb.append(bodyText == null ? "" : bodyText).append("\n\n");
        sb.append("- ").append(schoolName);
        if (!contact.isBlank()) sb.append(" (").append(contact).append(")");
        return sb.toString();
    }

    private String contactLine(School school) {
        if (school == null) return "";
        if (school.getCommunicationsEmail() != null && !school.getCommunicationsEmail().isBlank()) return school.getCommunicationsEmail();
        if (school.getPhone() != null && !school.getPhone().isBlank()) return school.getPhone();
        return "";
    }

    private void sendEmails(List<String> recipients, String subject, String htmlBody, String plainBody, School school) {
        if (recipients.isEmpty()) return;
        // "noreply@srms.zm" is the dev-profile default when MAIL_USERNAME/MAIL_PASSWORD are
        // unset; production's own default is blank (see application-production.properties) —
        // check for both so an unconfigured mail account is caught in either profile instead of
        // attempting a send that would just fail per-recipient with a MailException.
        if (fromEmail == null || fromEmail.isBlank() || fromEmail.equals("noreply@srms.zm")) {
            log.warn("Email channel selected but MAIL_USERNAME / MAIL_PASSWORD not configured — skipping {} emails", recipients.size());
            return;
        }
        String replyTo = school != null ? school.getCommunicationsEmail() : null;
        int sent = 0;
        for (String to : recipients) {
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(plainBody, htmlBody);
                if (replyTo != null && !replyTo.isBlank()) {
                    helper.setReplyTo(replyTo);
                }
                mailSender.send(mime);
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
