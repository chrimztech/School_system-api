package com.srms.api.modules.hr.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.hr.entity.LeaveRequest;
import com.srms.api.modules.hr.entity.StaffRecord;
import com.srms.api.modules.hr.repository.LeaveRepository;
import com.srms.api.modules.hr.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HrService {

    private final StaffRepository staffRepository;
    private final LeaveRepository leaveRepository;

    public List<StaffRecord> getAllStaff(String schoolId) {
        return staffRepository.findBySchoolId(schoolId);
    }

    public StaffRecord getStaff(String schoolId, String id) {
        return staffRepository.findById(id)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("StaffRecord", id));
    }

    public StaffRecord createStaff(String schoolId, StaffRecord record) {
        record.setSchoolId(schoolId);
        long count = staffRepository.findBySchoolId(schoolId).size();
        record.setStaffNumber(schoolId.toUpperCase() + "-STAFF-" + String.format("%03d", count + 1));
        record.setStatus(StaffRecord.StaffStatus.ACTIVE);
        return staffRepository.save(record);
    }

    public StaffRecord updateStaff(String schoolId, String id, StaffRecord updated) {
        StaffRecord record = getStaff(schoolId, id);
        record.setDepartment(updated.getDepartment());
        record.setPosition(updated.getPosition());
        record.setQualifications(updated.getQualifications());
        record.setContractType(updated.getContractType());
        record.setSalary(updated.getSalary());
        record.setStatus(updated.getStatus());
        return staffRepository.save(record);
    }

    public void deleteStaff(String schoolId, String id) {
        StaffRecord record = getStaff(schoolId, id);
        record.setStatus(StaffRecord.StaffStatus.TERMINATED);
        staffRepository.save(record);
    }

    public List<LeaveRequest> getAllLeave(String schoolId) {
        return leaveRepository.findBySchoolId(schoolId);
    }

    public LeaveRequest submitLeave(String schoolId, LeaveRequest request) {
        request.setSchoolId(schoolId);
        request.setStatus(LeaveRequest.LeaveStatus.PENDING);
        request.setAppliedDate(LocalDate.now());
        return leaveRepository.save(request);
    }

    public LeaveRequest approveLeave(String schoolId, String id, String approvedBy) {
        LeaveRequest leave = leaveRepository.findById(id)
                .filter(l -> l.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
        leave.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);
        return leaveRepository.save(leave);
    }

    public LeaveRequest rejectLeave(String schoolId, String id) {
        LeaveRequest leave = leaveRepository.findById(id)
                .filter(l -> l.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
        leave.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        return leaveRepository.save(leave);
    }
}
