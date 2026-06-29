package com.srms.api.modules.communication.service;

import com.srms.api.modules.communication.entity.Announcement;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final JavaMailSender mailSender;

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
        String audience = ann.getAudience() != null ? ann.getAudience().toLowerCase() : "all";
        List<String> channels = parseChannels(ann.getChannels());

        List<String> emails = new ArrayList<>();
        List<String> phones = new ArrayList<>();

        // Resolve recipients by audience
        if (audience.contains("staff") || audience.contains("teacher")) {
            List<Teacher> teachers = teacherRepository.findBySchoolIdAndStatus(schoolId, Teacher.TeacherStatus.active);
            for (Teacher t : teachers) {
                if (t.getEmail() != null && !t.getEmail().isBlank()) emails.add(t.getEmail());
                if (t.getPhone() != null && !t.getPhone().isBlank()) phones.add(normalizePhone(t.getPhone()));
            }
        }

        if (audience.contains("parent") || audience.contains("all") || audience.contains("student")) {
            List<Student> students = studentRepository.findBySchoolIdAndStatus(schoolId, Student.StudentStatus.active);
            for (Student s : students) {
                if (s.getGuardianEmail() != null && !s.getGuardianEmail().isBlank())
                    emails.add(s.getGuardianEmail());
                if (s.getGuardianPhone() != null && !s.getGuardianPhone().isBlank())
                    phones.add(normalizePhone(s.getGuardianPhone()));
                if (s.getGuardianAltPhone() != null && !s.getGuardianAltPhone().isBlank())
                    phones.add(normalizePhone(s.getGuardianAltPhone()));
                // Students themselves
                if (audience.contains("student") || audience.contains("all")) {
                    if (s.getStudentEmail() != null && !s.getStudentEmail().isBlank())
                        emails.add(s.getStudentEmail());
                }
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
