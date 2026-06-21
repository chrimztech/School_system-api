package com.srms.api.modules.visitor.service;

import com.srms.api.modules.visitor.entity.VisitorLog;
import com.srms.api.modules.visitor.repository.VisitorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class VisitorService {
    private final VisitorLogRepository repo;
    public List<VisitorLog> getAll(String schoolId) { return repo.findBySchoolIdOrderByCheckInTimeDesc(schoolId); }
    public VisitorLog checkIn(String schoolId, VisitorLog log) {
        log.setSchoolId(schoolId);
        log.setCheckInTime(LocalDateTime.now());
        log.setStatus("CHECKED_IN");
        return repo.save(log);
    }
    public VisitorLog checkOut(String schoolId, String id) {
        VisitorLog log = repo.findById(id).filter(l -> l.getSchoolId().equals(schoolId)).orElseThrow();
        log.setCheckOutTime(LocalDateTime.now());
        log.setStatus("CHECKED_OUT");
        return repo.save(log);
    }
}
