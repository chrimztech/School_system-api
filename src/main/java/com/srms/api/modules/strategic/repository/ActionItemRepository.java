package com.srms.api.modules.strategic.repository;

import com.srms.api.modules.strategic.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, String> {
    List<ActionItem> findBySchoolIdOrderByCreatedAtAsc(String schoolId);
    List<ActionItem> findBySchoolIdAndGoalId(String schoolId, String goalId);
}
