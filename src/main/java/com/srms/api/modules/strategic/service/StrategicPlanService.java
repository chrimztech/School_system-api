package com.srms.api.modules.strategic.service;

import com.srms.api.modules.strategic.entity.ActionItem;
import com.srms.api.modules.strategic.entity.StrategicGoal;
import com.srms.api.modules.strategic.entity.StrategicReview;
import com.srms.api.modules.strategic.repository.ActionItemRepository;
import com.srms.api.modules.strategic.repository.StrategicGoalRepository;
import com.srms.api.modules.strategic.repository.StrategicReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class StrategicPlanService {
    private final StrategicGoalRepository goalRepo;
    private final ActionItemRepository actionRepo;
    private final StrategicReviewRepository reviewRepo;

    public List<StrategicGoal> getGoals(String schoolId) { return goalRepo.findBySchoolIdOrderByCreatedAtAsc(schoolId); }
    public StrategicGoal createGoal(String schoolId, StrategicGoal goal) { goal.setSchoolId(schoolId); if (goal.getStatus() == null) goal.setStatus("On track"); return goalRepo.save(goal); }
    public StrategicGoal updateGoal(String schoolId, String id, StrategicGoal patch) {
        StrategicGoal goal = goalRepo.findById(id).filter(g -> g.getSchoolId().equals(schoolId)).orElseThrow(() -> new RuntimeException("Goal not found"));
        if (patch.getProgress() >= 0) goal.setProgress(Math.min(100, patch.getProgress()));
        if (patch.getStatus() != null) goal.setStatus(patch.getStatus());
        if (patch.getGoal() != null) goal.setGoal(patch.getGoal());
        return goalRepo.save(goal);
    }

    public List<ActionItem> getActions(String schoolId) { return actionRepo.findBySchoolIdOrderByCreatedAtAsc(schoolId); }
    public ActionItem createAction(String schoolId, ActionItem item) { item.setSchoolId(schoolId); if (item.getStatus() == null) item.setStatus("On track"); return actionRepo.save(item); }
    public ActionItem updateAction(String schoolId, String id, ActionItem patch) {
        ActionItem item = actionRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow(() -> new RuntimeException("Action not found"));
        if (patch.getStatus() != null) item.setStatus(patch.getStatus());
        return actionRepo.save(item);
    }

    public List<StrategicReview> getReviews(String schoolId) { return reviewRepo.findBySchoolIdOrderByReviewDateDesc(schoolId); }
    public StrategicReview createReview(String schoolId, StrategicReview review) { review.setSchoolId(schoolId); return reviewRepo.save(review); }
}
