package com.srms.api.modules.teacher.service;
import com.srms.api.common.BulkImportResult;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.teacher.dto.TeacherDto;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    public List<Teacher> findAll(String schoolId) { return teacherRepository.findBySchoolId(schoolId); }
    public Teacher findById(String id, String schoolId) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
    }
    public Teacher create(String schoolId, TeacherDto dto) {
        Teacher t = new Teacher();
        t.setSchoolId(schoolId);
        long count = teacherRepository.countBySchoolIdAndStatus(schoolId, Teacher.TeacherStatus.active);
        t.setStaffNumber("STF-" + schoolId.toUpperCase() + "-" + String.format("%03d", count + 1));
        t.setStatus(Teacher.TeacherStatus.active);
        mapDto(t, dto);
        return teacherRepository.save(t);
    }
    public BulkImportResult bulkCreate(String schoolId, List<TeacherDto> dtos) {
        int imported = 0;
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            try {
                create(schoolId, dtos.get(i));
                imported++;
            } catch (Exception e) {
                errors.add(new BulkImportResult.RowError(i, e.getMessage()));
            }
        }
        return new BulkImportResult(imported, errors);
    }

    public Teacher update(String id, String schoolId, TeacherDto dto) {
        Teacher t = findById(id, schoolId); mapDto(t, dto); return teacherRepository.save(t);
    }
    public void delete(String id, String schoolId) {
        Teacher t = findById(id, schoolId); t.setStatus(Teacher.TeacherStatus.inactive); teacherRepository.save(t);
    }
    private void mapDto(Teacher t, TeacherDto dto) {
        if (dto.getFirstName() != null) t.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) t.setLastName(dto.getLastName());
        if (dto.getEmail() != null) t.setEmail(dto.getEmail());
        if (dto.getPhone() != null) t.setPhone(dto.getPhone());
        if (dto.getSubject() != null) t.setSubject(dto.getSubject());
        if (dto.getQualification() != null) t.setQualification(dto.getQualification());
        if (dto.getDepartment() != null) t.setDepartment(dto.getDepartment());
        if (dto.getDateJoined() != null) t.setDateJoined(dto.getDateJoined());
        if (dto.getGender() != null) t.setGender(dto.getGender());
        if (dto.getNationalId() != null) t.setNationalId(dto.getNationalId());
        if (dto.getStatus() != null) t.setStatus(Teacher.TeacherStatus.valueOf(dto.getStatus()));
        if (dto.getSalary() > 0) t.setSalary(dto.getSalary());
        t.setEmergencyContactName(dto.getEmergencyContactName());
        t.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        t.setProfessionalLicenseNo(dto.getProfessionalLicenseNo());
        t.setTeachingExperienceYears(dto.getTeachingExperienceYears());
        t.setBankName(dto.getBankName());
        t.setBankAccount(dto.getBankAccount());
        t.setAddress(dto.getAddress());
    }
}
