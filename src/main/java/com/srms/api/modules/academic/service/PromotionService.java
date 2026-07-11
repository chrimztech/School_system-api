package com.srms.api.modules.academic.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.dto.PromotionRequest;
import com.srms.api.modules.academic.dto.PromotionResult;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.alumni.entity.AlumniRecord;
import com.srms.api.modules.alumni.repository.AlumniRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {
    private final SchoolClassRepository classRepository;
    private final ClassEnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final AlumniRepository alumniRepository;

    private record Resolved(PromotionRequest.Item item, ClassEnrolment enrolment, Student student, SchoolClass destinationClass) {}

    public PromotionResult promote(String schoolId, PromotionRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("No students selected for promotion");
        }
        SchoolClass sourceClass = classRepository.findByIdAndSchoolId(request.getSourceClassId(), schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("SchoolClass", request.getSourceClassId()));

        List<Resolved> resolved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (PromotionRequest.Item item : request.getItems()) {
            ClassEnrolment enrolment = enrolmentRepository.findById(item.getEnrolmentId()).orElse(null);
            if (enrolment == null || !enrolment.getClassId().equals(sourceClass.getId()) || !enrolment.getSchoolId().equals(schoolId)) {
                errors.add("Enrolment not found for student " + item.getStudentId());
                continue;
            }
            if (!"ACTIVE".equals(enrolment.getStatus())) {
                errors.add(enrolment.getStudentName() + " is not an active enrolment in this class");
                continue;
            }
            Student student = studentRepository.findByIdAndSchoolId(item.getStudentId(), schoolId).orElse(null);
            if (student == null) {
                errors.add("Student not found: " + item.getStudentId());
                continue;
            }
            SchoolClass destinationClass = null;
            if (!item.isGraduate()) {
                if (item.getDestinationClassId() == null || item.getDestinationClassId().isBlank()) {
                    errors.add(enrolment.getStudentName() + " has no destination class selected");
                    continue;
                }
                destinationClass = classRepository.findByIdAndSchoolId(item.getDestinationClassId(), schoolId).orElse(null);
                if (destinationClass == null) {
                    errors.add("Destination class not found for " + enrolment.getStudentName());
                    continue;
                }
                if (enrolmentRepository.existsByClassIdAndStudentIdAndAcademicYear(
                        destinationClass.getId(), student.getId(), destinationClass.getAcademicYear())) {
                    errors.add(enrolment.getStudentName() + " is already enrolled in " + destinationClass.getName()
                        + " for " + destinationClass.getAcademicYear());
                    continue;
                }
            }
            resolved.add(new Resolved(item, enrolment, student, destinationClass));
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }

        int promoted = 0;
        int graduated = 0;
        for (Resolved r : resolved) {
            if (r.item().isGraduate()) {
                r.student().setStatus(Student.StudentStatus.graduated);
                studentRepository.save(r.student());

                AlumniRecord alumni = AlumniRecord.builder()
                    .schoolId(schoolId)
                    .firstName(r.student().getFirstName())
                    .lastName(r.student().getLastName())
                    .admissionNumber(r.student().getAdmissionNumber())
                    .graduationYear(parseYear(sourceClass.getAcademicYear(), request.getTargetAcademicYear()))
                    .lastGrade(sourceClass.getGrade())
                    .status("ACTIVE")
                    .build();
                alumniRepository.save(alumni);

                r.enrolment().setStatus("COMPLETED");
                enrolmentRepository.save(r.enrolment());
                decrementEnrolment(sourceClass);
                graduated++;
            } else {
                SchoolClass dest = r.destinationClass();
                ClassEnrolment newEnrolment = ClassEnrolment.builder()
                    .schoolId(schoolId)
                    .classId(dest.getId())
                    .studentId(r.student().getId())
                    .studentName((r.student().getFirstName() + " " + r.student().getLastName()).trim())
                    .grade(String.valueOf(dest.getGrade()))
                    .academicYear(dest.getAcademicYear())
                    .status("ACTIVE")
                    .build();
                enrolmentRepository.save(newEnrolment);
                incrementEnrolment(dest);

                r.student().setGrade(dest.getGrade());
                r.student().setSection(dest.getSection());
                studentRepository.save(r.student());

                r.enrolment().setStatus("PROMOTED");
                enrolmentRepository.save(r.enrolment());
                decrementEnrolment(sourceClass);
                promoted++;
            }
        }

        return new PromotionResult(promoted, graduated);
    }

    private void incrementEnrolment(SchoolClass schoolClass) {
        schoolClass.setCurrentEnrolment(schoolClass.getCurrentEnrolment() + 1);
        classRepository.save(schoolClass);
    }

    private void decrementEnrolment(SchoolClass schoolClass) {
        schoolClass.setCurrentEnrolment(Math.max(0, schoolClass.getCurrentEnrolment() - 1));
        classRepository.save(schoolClass);
    }

    private int parseYear(String primary, String fallback) {
        try {
            return Integer.parseInt(primary);
        } catch (Exception e) {
            try {
                return Integer.parseInt(fallback);
            } catch (Exception e2) {
                return java.time.Year.now().getValue();
            }
        }
    }
}
