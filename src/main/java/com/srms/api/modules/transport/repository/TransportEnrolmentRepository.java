package com.srms.api.modules.transport.repository;

import com.srms.api.modules.transport.entity.TransportEnrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransportEnrolmentRepository extends JpaRepository<TransportEnrolment, String> {
    List<TransportEnrolment> findBySchoolId(String schoolId);
    List<TransportEnrolment> findBySchoolIdAndStatus(String schoolId, TransportEnrolment.Status status);
}
