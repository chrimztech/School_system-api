package com.srms.api.modules.activities.service;

import com.srms.api.modules.activities.entity.Activity;
import com.srms.api.modules.activities.entity.ActivityEnrolment;
import com.srms.api.modules.activities.repository.ActivityEnrolmentRepository;
import com.srms.api.modules.activities.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class ActivitiesService {
    private final ActivityRepository activityRepo;
    private final ActivityEnrolmentRepository enrolmentRepo;

    public List<Activity> getAll(String schoolId) { return activityRepo.findBySchoolId(schoolId); }
    public Activity create(String schoolId, Activity activity) { activity.setSchoolId(schoolId); if (activity.getStatus() == null) activity.setStatus("ACTIVE"); return activityRepo.save(activity); }
    public Activity update(String schoolId, String id, Activity updated) {
        Activity a = activityRepo.findById(id).filter(x -> x.getSchoolId().equals(schoolId)).orElseThrow();
        a.setName(updated.getName());
        a.setType(updated.getType());
        a.setCategory(updated.getCategory());
        a.setDescription(updated.getDescription());
        a.setCoordinator(updated.getCoordinator());
        a.setVenue(updated.getVenue());
        a.setMeetingSchedule(updated.getMeetingSchedule());
        a.setMeetingDay(updated.getMeetingDay());
        a.setMeetingTime(updated.getMeetingTime());
        a.setMeetingDuration(updated.getMeetingDuration());
        a.setStatus(updated.getStatus());
        a.setTargetGroup(updated.getTargetGroup());
        a.setStartDate(updated.getStartDate());
        a.setBudgetAllocated(updated.getBudgetAllocated());
        a.setMembershipFee(updated.getMembershipFee());
        a.setInsuranceRequired(updated.getInsuranceRequired());
        a.setClubConstitutionRef(updated.getClubConstitutionRef());
        a.setMaxParticipants(updated.getMaxParticipants());
        return activityRepo.save(a);
    }
    public void delete(String schoolId, String id) { activityRepo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).ifPresent(activityRepo::delete); }

    public List<ActivityEnrolment> getEnrolments(String schoolId, String activityId) { return enrolmentRepo.findBySchoolIdAndActivityId(schoolId, activityId); }
    public ActivityEnrolment enrol(String schoolId, ActivityEnrolment enrolment) {
        enrolment.setSchoolId(schoolId);
        if (enrolment.getEnrolmentDate() == null) enrolment.setEnrolmentDate(LocalDate.now());
        enrolment.setStatus("ACTIVE");
        return enrolmentRepo.save(enrolment);
    }
}
