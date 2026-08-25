package com.srms.api.modules.communication.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.communication.dto.AnnouncementDto;
import com.srms.api.modules.communication.dto.MessageDto;
import com.srms.api.modules.communication.entity.Announcement;
import com.srms.api.modules.communication.entity.Message;
import com.srms.api.modules.communication.repository.AnnouncementRepository;
import com.srms.api.modules.communication.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunicationService {

    private final AnnouncementRepository announcementRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;

    // ── Announcements ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncements(String schoolId) {
        return announcementRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    public Announcement createAnnouncement(String schoolId, AnnouncementDto dto) {
        Announcement ann = Announcement.builder()
                .schoolId(schoolId)
                .title(dto.getTitle())
                .body(dto.getBody())
                .audience(dto.getAudience())
                .channels(dto.getChannels())
                .publishDate(dto.getPublishDate() != null ? dto.getPublishDate() : LocalDate.now())
                .createdBy(dto.getCreatedBy())
                .active(true)
                .priority(dto.getPriority())
                .language(dto.getLanguage())
                .requireAck(dto.isRequireAck())
                .scheduledAt(dto.getScheduledAt())
                .build();
        Announcement saved = announcementRepository.save(ann);
        // Dispatch after transaction commits so the row is visible to async thread
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.dispatch(saved);
            }
        });
        return saved;
    }

    public Announcement updateAnnouncement(String schoolId, String id, AnnouncementDto dto) {
        Announcement ann = announcementRepository.findById(id)
                .filter(a -> a.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        ann.setTitle(dto.getTitle());
        ann.setBody(dto.getBody());
        ann.setAudience(dto.getAudience());
        ann.setChannels(dto.getChannels());
        if (dto.getPublishDate() != null) ann.setPublishDate(dto.getPublishDate());
        ann.setActive(dto.isActive());
        ann.setPriority(dto.getPriority());
        ann.setLanguage(dto.getLanguage());
        ann.setRequireAck(dto.isRequireAck());
        ann.setScheduledAt(dto.getScheduledAt());
        return announcementRepository.save(ann);
    }

    public void deleteAnnouncement(String schoolId, String id) {
        Announcement ann = announcementRepository.findById(id)
                .filter(a -> a.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        announcementRepository.delete(ann);
    }

    @Transactional(readOnly = true)
    public long getSmsBalance() {
        return notificationService.getSmsBalance();
    }

    // ── Messages ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Message> getMessages(String schoolId) {
        return messageRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessagesPaged(String schoolId, Pageable pageable) {
        return messageRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    public Message sendMessage(String schoolId, MessageDto dto) {
        Message msg = Message.builder()
                .schoolId(schoolId)
                .senderEmail(dto.getSenderEmail())
                .senderName(dto.getSenderName())
                .recipientEmail(dto.getRecipientEmail())
                .studentId(dto.getStudentId())
                .studentName(dto.getStudentName())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .status("OPEN")
                .build();
        return messageRepository.save(msg);
    }

    public Message replyToMessage(String schoolId, String id, String replyBody) {
        Message msg = messageRepository.findById(id)
                .filter(m -> m.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Message", id));
        msg.setReplyBody(replyBody);
        msg.setStatus("REPLIED");
        msg.setRepliedAt(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    public Message closeMessage(String schoolId, String id) {
        Message msg = messageRepository.findById(id)
                .filter(m -> m.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("Message", id));
        msg.setStatus("CLOSED");
        return messageRepository.save(msg);
    }
}
