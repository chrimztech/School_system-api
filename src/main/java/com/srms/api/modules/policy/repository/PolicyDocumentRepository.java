package com.srms.api.modules.policy.repository;

import com.srms.api.modules.policy.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, String> {
    List<PolicyDocument> findBySchoolIdOrderByUpdatedAtDesc(String schoolId);
}
