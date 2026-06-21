package com.srms.api.modules.calendar.service;

import com.srms.api.modules.calendar.entity.CalendarEvent;
import com.srms.api.modules.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class CalendarService {
    private final CalendarEventRepository repo;
    public List<CalendarEvent> list(String schoolId) { return repo.findBySchoolIdOrderByEventDateAsc(schoolId); }
    public CalendarEvent create(String schoolId, CalendarEvent e) { e.setSchoolId(schoolId); return repo.save(e); }
    public void delete(String schoolId, String id) { repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
