package com.srms.api.modules.curriculum.repository;

import com.srms.api.modules.curriculum.entity.SchemeWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeWeekRepository extends JpaRepository<SchemeWeek, String> {
    List<SchemeWeek> findBySchemeIdOrderByWeekNumberAsc(String schemeId);
    Optional<SchemeWeek> findByIdAndSchemeId(String id, String schemeId);
    void deleteBySchemeId(String schemeId);
}
