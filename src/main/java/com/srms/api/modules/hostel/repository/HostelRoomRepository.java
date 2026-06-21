package com.srms.api.modules.hostel.repository;

import com.srms.api.modules.hostel.entity.HostelRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HostelRoomRepository extends JpaRepository<HostelRoom, String> {
    List<HostelRoom> findBySchoolId(String schoolId);
    List<HostelRoom> findBySchoolIdAndStatus(String schoolId, String status);
}
