package com.srms.api.modules.welfare.repository;

import com.srms.api.modules.welfare.entity.WelfareCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WelfareCaseRepository extends JpaRepository<WelfareCase, String> {
    List<WelfareCase> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
