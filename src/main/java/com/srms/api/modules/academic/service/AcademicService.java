package com.srms.api.modules.academic.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.dto.GradeOffsetFixResult;
import com.srms.api.modules.academic.entity.*;
import com.srms.api.modules.academic.repository.*;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional @Slf4j
public class AcademicService {
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassEnrolmentRepository enrolmentRepository;
    private final TeacherClassSubjectRepository teacherSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SchoolRepository schoolRepository;

    // ── Classes ──────────────────────────────────────────────────
    public List<SchoolClass> findAllClasses(String schoolId) { return classRepository.findBySchoolIdAndActiveTrue(schoolId); }

    /** Returns only the classes a teacher is assigned to (subject teacher or homeroom teacher). */
    public List<SchoolClass> findClassesByTeacherEmail(String schoolId, String email) {
        return teacherRepository.findByEmailIgnoreCaseAndSchoolId(email, schoolId).map(teacher -> {
            String tid = teacher.getId();
            Set<String> classIds = new HashSet<>();
            teacherSubjectRepository.findByTeacherIdAndSchoolId(tid, schoolId)
                .forEach(a -> classIds.add(a.getClassId()));
            List<SchoolClass> all = classRepository.findBySchoolIdAndActiveTrue(schoolId);
            // Also include classes where the teacher is the homeroom (class) teacher
            all.stream()
                .filter(c -> tid.equals(c.getClassTeacherId()))
                .map(SchoolClass::getId)
                .forEach(classIds::add);
            if (classIds.isEmpty()) return List.<SchoolClass>of();
            return all.stream().filter(c -> classIds.contains(c.getId())).collect(Collectors.toList());
        }).orElse(List.of());
    }

    /** Every class+subject a teacher is actually assigned to teach — the scoping source of truth
     * for what a TEACHER can create/grade assessments for and which subjects they see. */
    public List<TeacherClassSubject> findAssignmentsByTeacherEmail(String schoolId, String email) {
        return teacherRepository.findByEmailIgnoreCaseAndSchoolId(email, schoolId)
                .map(teacher -> teacherSubjectRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId))
                .orElse(List.of());
    }

    /** Returns only the students enrolled in the teacher's classes. */
    public List<Student> findStudentsByTeacherEmail(String schoolId, String email) {
        List<SchoolClass> teacherClasses = findClassesByTeacherEmail(schoolId, email);
        if (teacherClasses.isEmpty()) return List.of();
        Set<String> studentIds = teacherClasses.stream()
            .flatMap(c -> enrolmentRepository.findByClassIdAndSchoolId(c.getId(), schoolId).stream())
            .map(ClassEnrolment::getStudentId)
            .collect(Collectors.toCollection(HashSet::new));
        if (studentIds.isEmpty()) return List.of();
        return studentRepository.findBySchoolIdAndIdIn(schoolId, new ArrayList<>(studentIds));
    }
    public SchoolClass findClassById(String id, String schoolId) { return classRepository.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new ResourceNotFoundException("Class", id)); }
    public SchoolClass createClass(String schoolId, SchoolClass dto) { dto.setSchoolId(schoolId); dto.setActive(true); return classRepository.save(dto); }
    public SchoolClass updateClass(String id, String schoolId, SchoolClass dto) {
        SchoolClass c = findClassById(id, schoolId);
        if (dto.getName() != null) c.setName(dto.getName());
        if (dto.getSection() != null) c.setSection(dto.getSection());
        if (dto.getGrade() > 0) c.setGrade(dto.getGrade());
        if (dto.getRoom() != null) c.setRoom(dto.getRoom());
        if (dto.getCapacity() > 0) c.setCapacity(dto.getCapacity());
        if (dto.getClassTeacherId() != null) c.setClassTeacherId(dto.getClassTeacherId());
        if (dto.getClassTeacherName() != null) c.setClassTeacherName(dto.getClassTeacherName());
        return classRepository.save(c);
    }
    public void deleteClass(String id, String schoolId) { SchoolClass c = findClassById(id, schoolId); c.setActive(false); classRepository.save(c); }

    /**
     * One-time repair for a bug in the class-creation UI (classes.tsx): on a COMBINED/FULL
     * school, an O-Level/A-Level ("olevel"/"alevel" phase) class must store its grade at
     * raw 7-12 (+6 off the Form number) to stay distinguishable from primary Grade 1-6, which
     * shares the same raw 1-6 range — see the matching comment on formatGrade in the
     * frontend's lib/tenant.tsx. The UI used to skip that offset, so any such class (and any
     * student enrolled in one before the fix) may still be sitting at the wrong raw grade,
     * silently mislabeled as a primary grade everywhere the type-aware formatGrade is used
     * (report cards, fee structures, results analysis, communication targeting, ...).
     *
     * Safe to run more than once: once a class's grade is corrected to 7-12, it no longer
     * matches the "olevel/alevel phase with grade 1-6" condition below, so a second run finds
     * nothing left to touch. Pure PRIMARY/NURSERY/SECONDARY schools never hit the collision
     * this offset exists for, so this is a deliberate no-op for them.
     */
    public GradeOffsetFixResult fixSecondaryGradeOffset(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", schoolId));
        String type = school.getType();
        boolean combined = "COMBINED".equalsIgnoreCase(type) || "FULL".equalsIgnoreCase(type);
        List<String> details = new ArrayList<>();
        if (!combined) {
            details.add(school.getName() + " is a " + type
                    + " school — Form 1-6 and legacy Grade 7-12 are never offset there, so there is nothing to fix.");
            return new GradeOffsetFixResult(0, 0, details);
        }

        int classesFixed = 0;
        int studentsFixed = 0;
        for (SchoolClass cls : classRepository.findBySchoolId(schoolId)) {
            boolean isFormPhase = "olevel".equals(cls.getPhase()) || "alevel".equals(cls.getPhase());
            if (!isFormPhase || cls.getGrade() < 1 || cls.getGrade() > 6) continue;

            int oldGrade = cls.getGrade();
            int newGrade = oldGrade + 6;
            int studentsFixedHere = 0;
            for (ClassEnrolment enrolment : enrolmentRepository.findByClassIdAndSchoolId(cls.getId(), schoolId)) {
                Student student = studentRepository.findById(enrolment.getStudentId()).orElse(null);
                if (student != null && schoolId.equals(student.getSchoolId()) && student.getGrade() == oldGrade) {
                    student.setGrade(newGrade);
                    studentRepository.save(student);
                    studentsFixedHere++;
                }
            }

            cls.setGrade(newGrade);
            classRepository.save(cls);
            classesFixed++;
            studentsFixed += studentsFixedHere;
            details.add(cls.getName() + " (" + cls.getPhase() + "): raw grade " + oldGrade + " -> " + newGrade
                    + ", " + studentsFixedHere + " enrolled student(s) corrected");
        }

        if (classesFixed == 0) {
            details.add("No affected classes found for " + school.getName()
                    + " — nothing to fix (either already correct, or no O-Level/A-Level classes created yet).");
        }
        return new GradeOffsetFixResult(classesFixed, studentsFixed, details);
    }

    // ── Class enrolments (student → class) ───────────────────────
    public List<ClassEnrolment> getClassEnrolments(String classId, String schoolId) {
        return enrolmentRepository.findByClassIdAndSchoolId(classId, schoolId);
    }

    public ClassEnrolment enrolStudent(String classId, String schoolId, ClassEnrolment dto) {
        String year = dto.getAcademicYear() != null ? dto.getAcademicYear() : String.valueOf(java.time.Year.now().getValue());
        if (enrolmentRepository.existsByClassIdAndStudentIdAndAcademicYear(classId, dto.getStudentId(), year)) {
            throw new IllegalArgumentException("Student is already enrolled in this class for " + year);
        }
        enrolmentRepository.findByStudentIdAndSchoolId(dto.getStudentId(), schoolId).stream()
            .filter(e -> year.equals(e.getAcademicYear()) && "ACTIVE".equals(e.getStatus()) && !e.getClassId().equals(classId))
            .findFirst()
            .ifPresent(e -> {
                String className = classRepository.findById(e.getClassId()).map(SchoolClass::getName).orElse("another class");
                throw new IllegalArgumentException(
                    (dto.getStudentName() != null ? dto.getStudentName() : "Student") + " is already enrolled in " + className
                        + " for " + year + ". Remove them from that class or use Promote to move them instead.");
            });
        dto.setClassId(classId);
        dto.setSchoolId(schoolId);
        dto.setAcademicYear(year);
        dto.setStatus("ACTIVE");
        ClassEnrolment saved = enrolmentRepository.save(dto);
        // keep currentEnrolment count in sync
        classRepository.findByIdAndSchoolId(classId, schoolId).ifPresent(c -> {
            c.setCurrentEnrolment(c.getCurrentEnrolment() + 1);
            classRepository.save(c);
        });
        return saved;
    }

    public void removeEnrolment(String classId, String enrolmentId, String schoolId) {
        ClassEnrolment e = enrolmentRepository.findById(enrolmentId)
            .filter(en -> en.getClassId().equals(classId) && en.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new ResourceNotFoundException("ClassEnrolment", enrolmentId));
        enrolmentRepository.delete(e);
        classRepository.findByIdAndSchoolId(classId, schoolId).ifPresent(c -> {
            c.setCurrentEnrolment(Math.max(0, c.getCurrentEnrolment() - 1));
            classRepository.save(c);
        });
    }

    // ── Teacher-class-subject assignments ────────────────────────
    public List<TeacherClassSubject> getClassTeachers(String classId, String schoolId) {
        return teacherSubjectRepository.findByClassIdAndSchoolId(classId, schoolId);
    }

    public TeacherClassSubject assignTeacher(String classId, String schoolId, TeacherClassSubject dto) {
        dto.setClassId(classId);
        dto.setSchoolId(schoolId);
        return teacherSubjectRepository.save(dto);
    }

    public void removeTeacherAssignment(String classId, String assignmentId, String schoolId) {
        TeacherClassSubject t = teacherSubjectRepository.findById(assignmentId)
            .filter(a -> a.getClassId().equals(classId) && a.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new ResourceNotFoundException("TeacherClassSubject", assignmentId));
        teacherSubjectRepository.delete(t);
    }

    // ── Subjects ─────────────────────────────────────────────────
    public List<Subject> findAllSubjects(String schoolId) { return subjectRepository.findBySchoolIdAndActiveTrue(schoolId); }
    public Subject findSubjectById(String id, String schoolId) { return subjectRepository.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new ResourceNotFoundException("Subject", id)); }

    public Subject createSubject(String schoolId, Subject dto) {
        dto.setSchoolId(schoolId);
        dto.setActive(true);
        return subjectRepository.save(dto);
    }

    public Subject updateSubject(String id, String schoolId, Subject dto) {
        Subject s = findSubjectById(id, schoolId);
        if (dto.getName() != null) s.setName(dto.getName());
        if (dto.getCode() != null) s.setCode(dto.getCode());
        if (dto.getDepartment() != null) s.setDepartment(dto.getDepartment());
        if (dto.getPhase() != null) s.setPhase(dto.getPhase());
        if (dto.getPeriodsPerWeek() != null && dto.getPeriodsPerWeek() > 0) s.setPeriodsPerWeek(dto.getPeriodsPerWeek());
        if (dto.getGradeFrom() != null && dto.getGradeFrom() > 0) s.setGradeFrom(dto.getGradeFrom());
        if (dto.getGradeTo() != null && dto.getGradeTo() > 0) s.setGradeTo(dto.getGradeTo());
        if (dto.getDescription() != null) s.setDescription(dto.getDescription());
        s.setCompulsory(dto.isCompulsory());
        return subjectRepository.save(s);
    }

    public List<Subject> bulkCreateSubjects(String schoolId, List<Subject> subjects) {
        List<Subject> created = new java.util.ArrayList<>();
        for (Subject dto : subjects) {
            String phase = dto.getPhase() != null ? dto.getPhase() : "secondary";
            if (!subjectRepository.existsBySchoolIdAndCodeAndPhase(schoolId, dto.getCode(), phase)) {
                dto.setSchoolId(schoolId);
                dto.setPhase(phase);
                dto.setActive(true);
                created.add(subjectRepository.save(dto));
            }
        }
        return created;
    }

    public void deleteSubject(String id, String schoolId) { Subject s = findSubjectById(id, schoolId); s.setActive(false); subjectRepository.save(s); }

    // ── Departments ───────────────────────────────────────────────
    public List<Department> findAllDepartments(String schoolId) {
        return departmentRepository.findBySchoolIdAndActiveTrue(schoolId);
    }

    public Department findDepartmentById(String id, String schoolId) {
        return departmentRepository.findByIdAndSchoolId(id, schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    public Department createDepartment(String schoolId, Department dto) {
        if (departmentRepository.existsBySchoolIdAndName(schoolId, dto.getName())) {
            throw new IllegalArgumentException("Department '" + dto.getName() + "' already exists");
        }
        dto.setSchoolId(schoolId);
        dto.setActive(true);
        // A department starts with no HOD by design: staff are added to it first, and a
        // head is only ever promoted afterwards from among that department's own staff
        // (see setDepartmentHead / updateDepartment).
        dto.setHeadTeacherId(null);
        return departmentRepository.save(dto);
    }

    public Department updateDepartment(String id, String schoolId, Department dto) {
        Department d = findDepartmentById(id, schoolId);
        if (dto.getName() != null) d.setName(dto.getName());
        if (dto.getCode() != null) d.setCode(dto.getCode());
        if (dto.getDescription() != null) d.setDescription(dto.getDescription());
        if (dto.getHeadTeacherId() != null) {
            setDepartmentHead(d, schoolId, dto.getHeadTeacherId().isBlank() ? null : dto.getHeadTeacherId());
        }
        return departmentRepository.save(d);
    }

    public void deleteDepartment(String id, String schoolId) {
        Department d = findDepartmentById(id, schoolId);
        d.setActive(false);
        if (d.getHeadTeacherId() != null) {
            String previousHeadTeacherId = d.getHeadTeacherId();
            d.setHeadTeacherId(null);
            demoteIfNoLongerHead(previousHeadTeacherId, schoolId, id);
        }
        departmentRepository.save(d);
    }

    /** Sets (or clears, if newHeadTeacherId is null) the department's head, keeping the
     * corresponding teacher's login role (HOD ⇄ TEACHER) in sync. */
    private void setDepartmentHead(Department d, String schoolId, String newHeadTeacherId) {
        String previousHeadTeacherId = d.getHeadTeacherId();
        if (Objects.equals(previousHeadTeacherId, newHeadTeacherId)) return;

        if (newHeadTeacherId != null) {
            Teacher teacher = teacherRepository.findByIdAndSchoolId(newHeadTeacherId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", newHeadTeacherId));
            if (!d.getName().equalsIgnoreCase(teacher.getDepartment())) {
                throw new BusinessException("Teacher must belong to the department before becoming HOD");
            }
            promoteToHod(teacher);
        }
        d.setHeadTeacherId(newHeadTeacherId);

        if (previousHeadTeacherId != null) {
            demoteIfNoLongerHead(previousHeadTeacherId, schoolId, d.getId());
        }
    }

    private void promoteToHod(Teacher teacher) {
        if (teacher.getEmail() == null) return;
        userRepository.findByEmail(teacher.getEmail()).ifPresent(u -> {
            if (u.getRole() == AppUser.UserRole.TEACHER) {
                u.setRole(AppUser.UserRole.HOD);
                userRepository.save(u);
            }
        });
    }

    /** Demotes a teacher's login back to TEACHER once they no longer head any active department. */
    private void demoteIfNoLongerHead(String teacherId, String schoolId, String excludeDeptId) {
        boolean stillHeadsAnother = departmentRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
            .anyMatch(dep -> !dep.getId().equals(excludeDeptId) && teacherId.equals(dep.getHeadTeacherId()));
        if (stillHeadsAnother) return;

        teacherRepository.findByIdAndSchoolId(teacherId, schoolId).ifPresent(teacher -> {
            if (teacher.getEmail() == null) return;
            userRepository.findByEmail(teacher.getEmail()).ifPresent(u -> {
                if (u.getRole() == AppUser.UserRole.HOD) {
                    u.setRole(AppUser.UserRole.TEACHER);
                    userRepository.save(u);
                }
            });
        });
    }

    // ── Academic terms (when each term starts/ends) ─────────────────────────
    public List<AcademicTerm> findAllTerms(String schoolId) {
        return academicTermRepository.findBySchoolIdOrderByAcademicYearDescTermAsc(schoolId);
    }

    public AcademicTerm createTerm(String schoolId, AcademicTerm dto) {
        if (dto.getTerm() < 1 || dto.getTerm() > 3) {
            throw new BusinessException("Term must be 1, 2, or 3");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new BusinessException("Start date and end date are required");
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BusinessException("End date must be after the start date");
        }
        if (academicTermRepository.findBySchoolIdAndAcademicYearAndTerm(schoolId, dto.getAcademicYear(), dto.getTerm()).isPresent()) {
            throw new BusinessException("Term " + dto.getTerm() + " for " + dto.getAcademicYear() + " is already defined — edit it instead");
        }
        dto.setSchoolId(schoolId);
        AcademicTerm saved = academicTermRepository.save(dto);
        syncCurrentTermFromCalendar(schoolId);
        return saved;
    }

    public AcademicTerm updateTerm(String id, String schoolId, AcademicTerm patch) {
        AcademicTerm term = academicTermRepository.findById(id)
            .filter(t -> t.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new ResourceNotFoundException("AcademicTerm", id));
        LocalDate nextStart = patch.getStartDate() != null ? patch.getStartDate() : term.getStartDate();
        LocalDate nextEnd = patch.getEndDate() != null ? patch.getEndDate() : term.getEndDate();
        if (!nextEnd.isAfter(nextStart)) {
            throw new BusinessException("End date must be after the start date");
        }
        term.setStartDate(nextStart);
        term.setEndDate(nextEnd);
        if (patch.getName() != null) term.setName(patch.getName());
        AcademicTerm saved = academicTermRepository.save(term);
        syncCurrentTermFromCalendar(schoolId);
        return saved;
    }

    public void deleteTerm(String id, String schoolId) {
        AcademicTerm term = academicTermRepository.findById(id)
            .filter(t -> t.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new ResourceNotFoundException("AcademicTerm", id));
        academicTermRepository.delete(term);
    }

    /**
     * Auto-advances School.currentTerm/currentYear off the academic term calendar, so a school
     * that's actually filled in real term dates doesn't need an admin to remember to bump the
     * "Current term" number in Settings by hand three times a year — see AcademicTerm's javadoc,
     * this is what makes that comment's stated intent actually true.
     *
     * Deliberately conservative: if today's date isn't covered by any defined term for this
     * school (calendar not fully filled in yet, or a real gap between terms — school holidays,
     * say), this leaves School.currentTerm untouched rather than guessing. An admin can always
     * still set it directly in Settings; this only overrides that once the calendar actually
     * says something different for today's date, and does nothing before the calendar is
     * meaningfully populated at all.
     */
    public void syncCurrentTermFromCalendar(String schoolId) {
        LocalDate today = LocalDate.now();
        List<AcademicTerm> covering = academicTermRepository
                .findBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(schoolId, today, today);
        if (covering.isEmpty()) return;
        AcademicTerm current = covering.get(0);
        School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) return;
        if (school.getCurrentTerm() == current.getTerm() && school.getCurrentYear() == current.getAcademicYear()) {
            return;
        }
        log.info("Auto-advancing school {} to term {} of {} per the academic term calendar (was term {} of {})",
                schoolId, current.getTerm(), current.getAcademicYear(), school.getCurrentTerm(), school.getCurrentYear());
        school.setCurrentTerm(current.getTerm());
        school.setCurrentYear(current.getAcademicYear());
        schoolRepository.save(school);
    }

    /**
     * Catches the case createTerm()/updateTerm() can't: a school's calendar already covers
     * today, and the rollover happens purely because a day passed, with no one editing the
     * calendar to trigger it. Runs early each morning so a fresh term is in effect before a
     * school's day starts. No cross-instance lock needed — see BackupService's scheduled job
     * for why a plain re-check like this is safe to run redundantly: this one just re-derives
     * the correct term from the calendar and writes it if it actually differs, so two instances
     * racing on the same school converge on the same value rather than compounding.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Africa/Harare")
    public void syncAllSchoolsCurrentTerm() {
        for (School school : schoolRepository.findByActiveTrue()) {
            try {
                syncCurrentTermFromCalendar(school.getId());
            } catch (Exception e) {
                log.error("Term calendar sync failed for school {}", school.getId(), e);
            }
        }
    }
}
