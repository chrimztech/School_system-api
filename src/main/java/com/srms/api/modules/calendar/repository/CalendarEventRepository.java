package com.srms.api.modules.calendar.repository;

import com.srms.api.modules.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, String> {
    List<CalendarEvent> findBySchoolIdOrderByEventDateAsc(String schoolId);
}
