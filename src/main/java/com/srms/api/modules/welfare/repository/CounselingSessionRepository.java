package com.srms.api.modules.welfare.repository;

import com.srms.api.modules.welfare.entity.CounselingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CounselingSessionRepository extends JpaRepository<CounselingSession, String> {
    List<CounselingSession> findBySchoolIdOrderBySessionDateDesc(String schoolId);
}
