package com.srms.api.modules.admission.repository;

import com.srms.api.modules.admission.entity.AdmissionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdmissionRepository extends JpaRepository<AdmissionApplication, String> {
    List<AdmissionApplication> findBySchoolIdOrderBySubmittedDateDesc(String schoolId);
    List<AdmissionApplication> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolId(String schoolId);
}
