package com.srms.api.modules.communication.repository;

import com.srms.api.modules.communication.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    Page<Message> findBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
    List<Message> findBySchoolIdAndStatus(String schoolId, String status);
    List<Message> findBySchoolIdAndStudentId(String schoolId, String studentId);
}
