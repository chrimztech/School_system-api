package com.srms.api.modules.lostfound.repository;

import com.srms.api.modules.lostfound.entity.LostFoundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LostFoundRepository extends JpaRepository<LostFoundItem, String> {
    List<LostFoundItem> findBySchoolIdOrderByFoundDateDesc(String schoolId);
    List<LostFoundItem> findBySchoolIdAndStatus(String schoolId, String status);
}
