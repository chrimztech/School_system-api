package com.srms.api.modules.student.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    public List<Student> findAll(String schoolId) {
        return studentRepository.findBySchoolId(schoolId);
    }

    public List<Student> findByGuardianEmail(String schoolId, String email) {
        return studentRepository.findBySchoolIdAndGuardianEmailIgnoreCase(schoolId, email);
    }

    public Student findById(String id, String schoolId) {
        return studentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    public Student create(String schoolId, StudentDto dto) {
        String shortCode = schoolRepository.findShortCodeById(schoolId)
                .map(String::toUpperCase)
                .orElse("STU");

        int year = Year.now().getValue();
        String prefix = shortCode + "-" + year + "-";

        // Count all students ever admitted under this prefix to get the next sequence
        long base = studentRepository.countBySchoolIdAndAdmissionNumberPrefix(schoolId, prefix);
        String admissionNumber;
        int attempt = 0;
        do {
            admissionNumber = prefix + String.format("%04d", base + 1 + attempt);
            attempt++;
        } while (studentRepository.existsByAdmissionNumber(admissionNumber) && attempt < 100);

        Student student = new Student();
        student.setSchoolId(schoolId);
        mapDto(student, dto);
        student.setAdmissionNumber(admissionNumber);
        if (student.getStatus() == null) {
            student.setStatus(Student.StudentStatus.active);
        }
        if (student.getAdmissionDate() == null || student.getAdmissionDate().isBlank()) {
            student.setAdmissionDate(LocalDate.now().toString());
        }
        student.setFeeBalance(0);
        return studentRepository.save(student);
    }

    public Student update(String id, String schoolId, StudentDto dto) {
        Student student = findById(id, schoolId);
        mapDto(student, dto);
        return studentRepository.save(student);
    }

    public void delete(String id, String schoolId) {
        Student student = findById(id, schoolId);
        student.setStatus(Student.StudentStatus.inactive);
        studentRepository.save(student);
    }

    private void mapDto(Student s, StudentDto dto) {
        if (dto.getFirstName() != null) s.setFirstName(dto.getFirstName());
        if (dto.getMiddleName() != null) s.setMiddleName(dto.getMiddleName());
        if (dto.getLastName() != null) s.setLastName(dto.getLastName());
        if (dto.getPreferredName() != null) s.setPreferredName(dto.getPreferredName());
        if (dto.getGrade() != null && dto.getGrade() > 0) s.setGrade(dto.getGrade());
        if (dto.getSection() != null) s.setSection(dto.getSection());
        if (dto.getAdmissionDate() != null) s.setAdmissionDate(dto.getAdmissionDate());
        if (dto.getDateOfBirth() != null) s.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) s.setGender(dto.getGender());
        if (dto.getNationality() != null) s.setNationality(dto.getNationality());
        if (dto.getNationalId() != null) s.setNationalId(dto.getNationalId());
        if (dto.getBirthCertificateNo() != null) s.setBirthCertificateNo(dto.getBirthCertificateNo());
        if (dto.getStudentPhone() != null) s.setStudentPhone(dto.getStudentPhone());
        if (dto.getStudentEmail() != null) s.setStudentEmail(dto.getStudentEmail());
        if (dto.getReligion() != null) s.setReligion(dto.getReligion());
        if (dto.getBloodGroup() != null) s.setBloodGroup(dto.getBloodGroup());
        if (dto.getMedicalConditions() != null) s.setMedicalConditions(dto.getMedicalConditions());
        if (dto.getAllergies() != null) s.setAllergies(dto.getAllergies());
        if (dto.getAddress() != null) s.setAddress(dto.getAddress());
        if (dto.getCity() != null) s.setCity(dto.getCity());
        if (dto.getGuardian() != null) s.setGuardian(dto.getGuardian());
        if (dto.getGuardianRelationship() != null) s.setGuardianRelationship(dto.getGuardianRelationship());
        if (dto.getGuardianPhone() != null) s.setGuardianPhone(dto.getGuardianPhone());
        if (dto.getGuardianAltPhone() != null) s.setGuardianAltPhone(dto.getGuardianAltPhone());
        if (dto.getGuardianEmail() != null) s.setGuardianEmail(dto.getGuardianEmail());
        if (dto.getGuardianOccupation() != null) s.setGuardianOccupation(dto.getGuardianOccupation());
        if (dto.getGuardianWorkplace() != null) s.setGuardianWorkplace(dto.getGuardianWorkplace());
        if (dto.getGuardianNationalId() != null) s.setGuardianNationalId(dto.getGuardianNationalId());
        if (dto.getGuardianAddress() != null) s.setGuardianAddress(dto.getGuardianAddress());
        if (dto.getEmergencyContactName() != null) s.setEmergencyContactName(dto.getEmergencyContactName());
        if (dto.getEmergencyContactRelationship() != null) s.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        if (dto.getEmergencyContactPhone() != null) s.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) s.setStatus(Student.StudentStatus.valueOf(dto.getStatus()));
    }
}