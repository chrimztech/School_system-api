package com.srms.api.modules.curriculum.repository;

import com.srms.api.modules.curriculum.entity.SchemeOfWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeOfWorkRepository extends JpaRepository<SchemeOfWork, String> {
    List<SchemeOfWork> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    Optional<SchemeOfWork> findByIdAndSchoolId(String id, String schoolId);
}
