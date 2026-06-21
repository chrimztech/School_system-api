package com.srms.api.modules.development.service;

import com.srms.api.modules.development.entity.StaffAppraisal;
import com.srms.api.modules.development.entity.StaffDevelopmentPlan;
import com.srms.api.modules.development.entity.StaffObservation;
import com.srms.api.modules.development.entity.TrainingRecord;
import com.srms.api.modules.development.repository.StaffAppraisalRepository;
import com.srms.api.modules.development.repository.StaffDevelopmentPlanRepository;
import com.srms.api.modules.development.repository.StaffObservationRepository;
import com.srms.api.modules.development.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class DevelopmentService {
    private final TrainingRepository repo;
    private final StaffAppraisalRepository appraisalRepository;
    private final StaffObservationRepository observationRepository;
    private final StaffDevelopmentPlanRepository planRepository;
    public List<TrainingRecord> list(String schoolId) { return repo.findBySchoolIdOrderByStartDateDesc(schoolId); }
    public TrainingRecord create(String schoolId, TrainingRecord t) { t.setSchoolId(schoolId); return repo.save(t); }
    public TrainingRecord update(String schoolId, String id, TrainingRecord updated) {
        TrainingRecord t = repo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        t.setStatus(updated.getStatus()); t.setHours(updated.getHours()); t.setEndDate(updated.getEndDate());
        return repo.save(t);
    }

    public List<StaffAppraisal> appraisals(String schoolId) { return appraisalRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public StaffAppraisal createAppraisal(String schoolId, StaffAppraisal appraisal) {
        appraisal.setSchoolId(schoolId);
        if (appraisal.getStatus() == null) appraisal.setStatus("Draft");
        return appraisalRepository.save(appraisal);
    }
    public StaffAppraisal updateAppraisal(String schoolId, String id, StaffAppraisal patch) {
        StaffAppraisal appraisal = appraisalRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getScore() != null) appraisal.setScore(patch.getScore());
        if (patch.getStatus() != null) appraisal.setStatus(patch.getStatus());
        if (patch.getReviewer() != null) appraisal.setReviewer(patch.getReviewer());
        if (patch.getCycle() != null) appraisal.setCycle(patch.getCycle());
        return appraisalRepository.save(appraisal);
    }

    public List<StaffObservation> observations(String schoolId) { return observationRepository.findBySchoolIdOrderByDateDescCreatedAtDesc(schoolId); }
    public StaffObservation createObservation(String schoolId, StaffObservation observation) {
        observation.setSchoolId(schoolId);
        return observationRepository.save(observation);
    }

    public List<StaffDevelopmentPlan> pdps(String schoolId) { return planRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId); }
    public StaffDevelopmentPlan createPdp(String schoolId, StaffDevelopmentPlan plan) {
        plan.setSchoolId(schoolId);
        return planRepository.save(plan);
    }
    public StaffDevelopmentPlan updatePdp(String schoolId, String id, StaffDevelopmentPlan patch) {
        StaffDevelopmentPlan plan = planRepository.findById(id).filter(item -> item.getSchoolId().equals(schoolId)).orElseThrow();
        if (patch.getGoals() != null) plan.setGoals(patch.getGoals());
        if (patch.getNextReview() != null) plan.setNextReview(patch.getNextReview());
        if (patch.getStatus() != null) plan.setStatus(patch.getStatus());
        if (patch.getSupportRequired() != null) plan.setSupportRequired(patch.getSupportRequired());
        return planRepository.save(plan);
    }
}
