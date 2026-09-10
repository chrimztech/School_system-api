package com.srms.api.modules.student.service;

import com.srms.api.common.BulkImportResult;
import com.srms.api.common.PhoneUtils;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.activities.repository.ActivityEnrolmentRepository;
import com.srms.api.modules.assessment.repository.PublishedTermGradeRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import com.srms.api.modules.assessment.repository.TermGradeRepository;
import com.srms.api.modules.attendance.repository.AttendanceRepository;
import com.srms.api.modules.communication.repository.MessageRepository;
import com.srms.api.modules.discipline.repository.DisciplineRepository;
import com.srms.api.modules.exam.repository.ExamCandidateRepository;
import com.srms.api.modules.fee.repository.FeePaymentRepository;
import com.srms.api.modules.fee.service.FeeService;
import com.srms.api.modules.health.repository.HealthRecordRepository;
import com.srms.api.modules.health.repository.HealthVisitRepository;
import com.srms.api.modules.hostel.repository.HostelAllocationRepository;
import com.srms.api.modules.hostel.repository.HostelLeaveRepository;
import com.srms.api.modules.report.repository.ReportCommentRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.student.dto.StudentDto;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.modules.student.repository.StudentSpecifications;
import com.srms.api.modules.transport.repository.TransportEnrolmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final FeeService feeService;
    private final ClassEnrolmentRepository classEnrolmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ResultRepository resultRepository;
    private final TermGradeRepository termGradeRepository;
    private final PublishedTermGradeRepository publishedTermGradeRepository;
    private final DisciplineRepository disciplineRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthVisitRepository healthVisitRepository;
    private final HostelAllocationRepository hostelAllocationRepository;
    private final HostelLeaveRepository hostelLeaveRepository;
    private final TransportEnrolmentRepository transportEnrolmentRepository;
    private final ActivityEnrolmentRepository activityEnrolmentRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MessageRepository messageRepository;

    public List<Student> findAll(String schoolId) {
        return studentRepository.findBySchoolId(schoolId);
    }

    public Page<Student> findAllPaged(String schoolId, Pageable pageable) {
        return studentRepository.findBySchoolId(schoolId, pageable);
    }

    public Page<Student> findAllPaged(String schoolId, String query, String status, Integer grade, Pageable pageable) {
        boolean anyFilter = (query != null && !query.isBlank()) || (status != null && !status.isBlank()) || grade != null;
        if (!anyFilter) return findAllPaged(schoolId, pageable);
        return studentRepository.findAll(StudentSpecifications.search(schoolId, query, status, grade), pageable);
    }

    public List<Student> findByGuardianEmail(String schoolId, String email) {
        return studentRepository.findBySchoolIdAndGuardianEmailIgnoreCase(schoolId, email);
    }

    /**
     * Guardian phone numbers are free-typed by school staff and not stored in one canonical
     * format, so matching happens in-memory against a normalized (spaces/dashes stripped)
     * form rather than an exact DB match. Lets phone-only parents (no email on file) still
     * see their own children's report cards.
     */
    public List<Student> findByGuardianPhone(String schoolId, String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isEmpty()) return List.of();
        return studentRepository.findBySchoolId(schoolId).stream()
                .filter(s -> normalized.equals(normalizePhone(s.getGuardianPhone()))
                        || normalized.equals(normalizePhone(s.getGuardianAltPhone())))
                .toList();
    }

    private String normalizePhone(String phone) {
        return PhoneUtils.normalize(phone);
    }

    /**
     * Rejects a registration that looks like the same real child being added a second time —
     * this is what was letting a duplicated pupil accrue its own separate fee balance and
     * show up as a second invoice to the same guardian. A bare first+last name match isn't
     * enough on its own to reject (common names are, well, common) — at least one
     * corroborating field (national ID, birth certificate number, date of birth, or guardian
     * phone) must also match an existing active record with the same name before this blocks
     * the save. Runs for both the single "Register pupil" form and every row of a CSV bulk
     * import, since create() is the one place both paths go through.
     */
    private void assertNotDuplicate(String schoolId, StudentDto dto) {
        String firstName = dto.getFirstName();
        String lastName = dto.getLastName();
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) return;

        List<Student> candidates = studentRepository.findBySchoolIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndStatusNot(
                schoolId, firstName.trim(), lastName.trim(), Student.StudentStatus.inactive);
        if (candidates.isEmpty()) return;

        String dob = dto.getDateOfBirth();
        String guardianPhone = normalizePhone(dto.getGuardianPhone());
        String nationalId = dto.getNationalId();
        String birthCert = dto.getBirthCertificateNo();

        for (Student existing : candidates) {
            boolean sameNationalId = nationalId != null && !nationalId.isBlank()
                    && nationalId.trim().equalsIgnoreCase(existing.getNationalId());
            boolean sameBirthCert = birthCert != null && !birthCert.isBlank()
                    && birthCert.trim().equalsIgnoreCase(existing.getBirthCertificateNo());
            boolean sameDob = dob != null && !dob.isBlank() && dob.equals(existing.getDateOfBirth());
            boolean sameGuardianPhone = !guardianPhone.isEmpty()
                    && guardianPhone.equals(normalizePhone(existing.getGuardianPhone()));

            if (sameNationalId || sameBirthCert || sameDob || sameGuardianPhone) {
                throw new BusinessException(
                        "A pupil named " + firstName.trim() + " " + lastName.trim()
                                + " is already registered (admission no. " + existing.getAdmissionNumber()
                                + "). If this is genuinely a different learner, make sure the name, date of "
                                + "birth, and guardian phone are entered distinctly before saving again.");
            }
        }
    }

    public Student findById(String id, String schoolId) {
        return studentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    public Student create(String schoolId, StudentDto dto) {
        assertNotDuplicate(schoolId, dto);
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
        student.setFeeBalance(billInitialTermFee(schoolId, student));
        return studentRepository.save(student);
    }

    public BulkImportResult bulkCreate(String schoolId, List<StudentDto> dtos) {
        int imported = 0;
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        List<BulkImportResult.CreatedRow> created = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            try {
                Student saved = create(schoolId, dtos.get(i));
                created.add(new BulkImportResult.CreatedRow(i, saved.getId()));
                imported++;
            } catch (Exception e) {
                errors.add(new BulkImportResult.RowError(i, e.getMessage()));
            }
        }
        return new BulkImportResult(imported, errors, created);
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

    /**
     * Permanently erases a pupil and every record tied to them by studentId — the soft
     * delete above (status -> inactive) is what "Delete" normally means in this app, kept
     * on purpose so a mistaken removal doesn't lose history. This is the separate, explicit
     * "permanently delete" action for when a record genuinely needs to be gone (a duplicate,
     * a test entry, a data-protection request) — irreversible, and the controller restricts
     * it to school-account-manager roles for that reason.
     *
     * Deliberately does NOT touch: the guardian's AppUser login (one guardian can have several
     * children — deleting one must not delete a login that still serves the others), or
     * WelfareCase/BursaryAward/BursaryApplication (those reference a student by free-typed
     * name text, not studentId, so there's no reliable link to clean up without risking a
     * false-positive match on an unrelated record that happens to share that name).
     */
    @Transactional
    public void deletePermanently(String id, String schoolId) {
        Student student = findById(id, schoolId);

        List<ClassEnrolment> enrolments = classEnrolmentRepository.findByStudentIdAndSchoolId(id, schoolId);
        for (ClassEnrolment enrolment : enrolments) {
            schoolClassRepository.findByIdAndSchoolId(enrolment.getClassId(), schoolId).ifPresent(c -> {
                c.setCurrentEnrolment(Math.max(0, c.getCurrentEnrolment() - 1));
                schoolClassRepository.save(c);
            });
        }
        classEnrolmentRepository.deleteAll(enrolments);

        resultRepository.deleteAll(resultRepository.findBySchoolIdAndStudentId(schoolId, id));
        termGradeRepository.deleteAll(termGradeRepository.findBySchoolIdAndStudentId(schoolId, id));
        publishedTermGradeRepository.deleteAll(publishedTermGradeRepository.findBySchoolIdAndStudentId(schoolId, id));
        disciplineRepository.deleteAll(disciplineRepository.findBySchoolIdAndStudentId(schoolId, id));
        healthRecordRepository.findBySchoolIdAndStudentId(schoolId, id).ifPresent(healthRecordRepository::delete);
        healthVisitRepository.deleteAll(healthVisitRepository.findBySchoolIdAndStudentId(schoolId, id));
        hostelAllocationRepository.deleteAll(hostelAllocationRepository.findBySchoolIdAndStudentId(schoolId, id));
        hostelLeaveRepository.deleteAll(hostelLeaveRepository.findBySchoolIdAndStudentId(schoolId, id));
        transportEnrolmentRepository.deleteAll(transportEnrolmentRepository.findBySchoolIdAndStudentId(schoolId, id));
        activityEnrolmentRepository.deleteAll(activityEnrolmentRepository.findBySchoolIdAndStudentId(schoolId, id));
        examCandidateRepository.deleteAll(examCandidateRepository.findBySchoolIdAndStudentId(schoolId, id));
        reportCommentRepository.deleteAll(reportCommentRepository.findBySchoolIdAndStudentId(schoolId, id));
        feePaymentRepository.deleteAll(feePaymentRepository.findBySchoolIdAndStudentId(schoolId, id));
        attendanceRepository.deleteAll(attendanceRepository.findBySchoolIdAndStudentIdOrderByDateDesc(schoolId, id));
        messageRepository.deleteAll(messageRepository.findBySchoolIdAndStudentId(schoolId, id));

        studentRepository.delete(student);
    }

    private double billInitialTermFee(String schoolId, Student student) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) return 0;
        return feeService.computeInitialBalance(schoolId, student.getGrade(), school.getCurrentTerm(), school.getCurrentYear(), school.getType(), student.getBoardingStatus());
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
        if (dto.getBoardingStatus() != null) s.setBoardingStatus(dto.getBoardingStatus());
        if (dto.getNeedsTransport() != null) s.setNeedsTransport(dto.getNeedsTransport());
    }
}