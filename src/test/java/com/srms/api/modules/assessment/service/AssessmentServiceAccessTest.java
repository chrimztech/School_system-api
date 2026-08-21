package com.srms.api.modules.assessment.service;

import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.academic.entity.Department;
import com.srms.api.modules.academic.entity.Subject;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.DepartmentRepository;
import com.srms.api.modules.academic.repository.SubjectRepository;
import com.srms.api.modules.academic.repository.TeacherClassSubjectRepository;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceAccessTest {
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private TermGradeService termGradeService;
    @Mock private GradingScaleService gradingScaleService;
    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherClassSubjectRepository teacherSubjectRepository;
    @Mock private ClassEnrolmentRepository classEnrolmentRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SubjectRepository subjectRepository;

    @InjectMocks private AssessmentService service;

    @Test
    void parentCannotOpenAssessmentOperations() {
        String schoolId = "school-1";
        String userId = "parent-1";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, "parent@example.com")));

        assertThrows(ForbiddenException.class,
                () -> service.findAllForActor(schoolId, null, null, userId, "PARENT"));

        verifyNoInteractions(assessmentRepository);
    }

    @Test
    void teacherCannotReadAnUnassignedAssessmentById() {
        String schoolId = "school-1";
        String userId = "teacher-1";
        AppUser user = user(userId, schoolId, "teacher@example.com");
        Assessment assessment = Assessment.builder()
                .schoolId(schoolId)
                .title("Mathematics test")
                .classId("class-1")
                .subjectName("Mathematics")
                .build();
        assessment.setId("assessment-1");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assessmentRepository.findByIdAndSchoolId("assessment-1", schoolId))
                .thenReturn(Optional.of(assessment));
        when(teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> service.findByIdForActor("assessment-1", schoolId, userId, "TEACHER"));
    }

    @Test
    void hodQueueContainsOnlySubjectsFromTheirDepartment() {
        String schoolId = "school-1";
        String userId = "hod-1";
        AppUser user = user(userId, schoolId, "hod@example.com");
        Teacher teacher = Teacher.builder().email(user.getEmail()).schoolId(schoolId).build();
        teacher.setId("teacher-1");
        Department department = Department.builder()
                .schoolId(schoolId)
                .name("Sciences")
                .headTeacherId(teacher.getId())
                .build();
        Assessment mathematics = Assessment.builder()
                .schoolId(schoolId).title("Math test").subjectName("Mathematics").build();
        Assessment english = Assessment.builder()
                .schoolId(schoolId).title("English test").subjectName("English").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assessmentRepository.findBySchoolIdOrderByDateDesc(schoolId))
                .thenReturn(List.of(mathematics, english));
        when(teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId))
                .thenReturn(Optional.of(teacher));
        when(departmentRepository.findBySchoolIdAndActiveTrue(schoolId))
                .thenReturn(List.of(department));
        when(subjectRepository.findBySchoolIdAndActiveTrue(schoolId))
                .thenReturn(List.of(
                        Subject.builder().schoolId(schoolId).name("Mathematics").department("Sciences").build(),
                        Subject.builder().schoolId(schoolId).name("English").department("Languages").build()));

        List<Assessment> visible = service.findAllForActor(schoolId, null, null, userId, "HOD");

        assertEquals(List.of(mathematics), visible);
    }

    private static AppUser user(String id, String schoolId, String email) {
        AppUser user = AppUser.builder()
                .schoolId(schoolId)
                .email(email)
                .name("Test User")
                .passwordHash("hash")
                .role(AppUser.UserRole.SCHOOL_ADMIN)
                .build();
        user.setId(id);
        return user;
    }
}
