package com.srms.api.modules.admission.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.admission.entity.AdmissionApplication;
import com.srms.api.modules.admission.repository.AdmissionRepository;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdmissionService {
    private final AdmissionRepository repo;
    private final StudentService studentService;

    public List<AdmissionApplication> getAll(String schoolId) {
        return repo.findBySchoolIdOrderBySubmittedDateDesc(schoolId);
    }

    public AdmissionApplication create(String schoolId, AdmissionApplication app) {
        app.setSchoolId(schoolId);
        app.setStatus("PENDING");
        app.setSubmittedDate(LocalDate.now());
        long count = repo.countBySchoolId(schoolId);
        app.setApplicationNumber("APP-" + LocalDate.now().getYear() + "-" + String.format("%04d", count + 1));
        return repo.save(app);
    }

    public AdmissionApplication update(String schoolId, String id, AdmissionApplication updated) {
        AdmissionApplication app = get(schoolId, id);
        mapMutableFields(app, updated);
        return repo.save(app);
    }

    public AdmissionApplication get(String schoolId, String id) {
        return repo.findById(id).filter(a -> a.getSchoolId().equals(schoolId)).orElseThrow();
    }

    public AdmissionApplication accept(String schoolId, String id) {
        AdmissionApplication app = get(schoolId, id);
        Student student = resolveOrCreateStudent(schoolId, app);
        LocalDate today = LocalDate.now();

        app.setStatus("ACCEPTED");
        if (app.getDecidedDate() == null) {
            app.setDecidedDate(today);
        }
        app.setEnrolledStudentId(student.getId());
        app.setEnrolledAdmissionNumber(student.getAdmissionNumber());
        if (app.getEnrolledDate() == null) {
            app.setEnrolledDate(today);
        }
        return repo.save(app);
    }

    public AdmissionApplication reject(String schoolId, String id) {
        AdmissionApplication app = get(schoolId, id);
        app.setStatus("REJECTED");
        app.setDecidedDate(LocalDate.now());
        return repo.save(app);
    }

    private Student resolveOrCreateStudent(String schoolId, AdmissionApplication app) {
        if (app.getEnrolledStudentId() != null && !app.getEnrolledStudentId().isBlank()) {
            try {
                return studentService.findById(app.getEnrolledStudentId(), schoolId);
            } catch (ResourceNotFoundException ignored) {
                // Recreate the learner record if the admission was linked to a missing student.
            }
        }

        StudentDto dto = new StudentDto();
        dto.setFirstName(app.getFirstName());
        dto.setMiddleName(app.getMiddleName());
        dto.setLastName(app.getLastName());
        dto.setPreferredName(app.getPreferredName());
        dto.setGrade(app.getApplyingForGrade());
        dto.setAdmissionDate(LocalDate.now().toString());
        dto.setDateOfBirth(app.getDateOfBirth());
        dto.setGender(app.getGender());
        dto.setNationality(app.getNationality());
        dto.setBirthCertificateNo(app.getBirthCertificateNo());
        dto.setMedicalConditions(app.getMedicalNotes());
        dto.setAddress(app.getAddress());
        dto.setCity(app.getCity());
        dto.setGuardian(app.getGuardianName());
        dto.setGuardianRelationship(app.getGuardianRelationship());
        dto.setGuardianPhone(app.getGuardianPhone());
        dto.setGuardianAltPhone(app.getGuardianAltPhone());
        dto.setGuardianEmail(app.getGuardianEmail());
        dto.setGuardianOccupation(app.getGuardianOccupation());
        dto.setGuardianWorkplace(app.getGuardianWorkplace());
        dto.setGuardianNationalId(app.getGuardianNationalId());
        dto.setGuardianAddress(app.getGuardianAddress());
        dto.setEmergencyContactName(app.getEmergencyContactName());
        dto.setEmergencyContactRelationship(app.getEmergencyContactRelationship());
        dto.setEmergencyContactPhone(app.getEmergencyContactPhone());
        dto.setStatus(Student.StudentStatus.active.name());
        return studentService.create(schoolId, dto);
    }

    private void mapMutableFields(AdmissionApplication app, AdmissionApplication updated) {
        app.setFirstName(updated.getFirstName());
        app.setMiddleName(updated.getMiddleName());
        app.setLastName(updated.getLastName());
        app.setPreferredName(updated.getPreferredName());
        app.setDateOfBirth(updated.getDateOfBirth());
        app.setGender(updated.getGender());
        app.setNationality(updated.getNationality());
        app.setBirthCertificateNo(updated.getBirthCertificateNo());
        app.setApplyingForGrade(updated.getApplyingForGrade());
        app.setPreviousSchool(updated.getPreviousSchool());
        app.setLastCompletedGrade(updated.getLastCompletedGrade());
        app.setAddress(updated.getAddress());
        app.setCity(updated.getCity());
        app.setGuardianName(updated.getGuardianName());
        app.setGuardianRelationship(updated.getGuardianRelationship());
        app.setGuardianPhone(updated.getGuardianPhone());
        app.setGuardianAltPhone(updated.getGuardianAltPhone());
        app.setGuardianEmail(updated.getGuardianEmail());
        app.setGuardianOccupation(updated.getGuardianOccupation());
        app.setGuardianWorkplace(updated.getGuardianWorkplace());
        app.setGuardianNationalId(updated.getGuardianNationalId());
        app.setGuardianAddress(updated.getGuardianAddress());
        app.setEmergencyContactName(updated.getEmergencyContactName());
        app.setEmergencyContactRelationship(updated.getEmergencyContactRelationship());
        app.setEmergencyContactPhone(updated.getEmergencyContactPhone());
        app.setSource(updated.getSource());
        app.setPriority(updated.getPriority());
        app.setMedicalNotes(updated.getMedicalNotes());
        app.setNotes(updated.getNotes());
        app.setStatus(updated.getStatus());
    }
}
