package com.srms.api.modules.academic.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.*;
import com.srms.api.modules.academic.repository.*;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class AcademicService {
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final ClassEnrolmentRepository enrolmentRepository;
    private final TeacherClassSubjectRepository teacherSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    // ── Classes ──────────────────────────────────────────────────
    public List<SchoolClass> findAllClasses(String schoolId) { return classRepository.findBySchoolIdAndActiveTrue(schoolId); }

    /** Returns only the classes a teacher is assigned to (subject teacher or homeroom teacher). */
    public List<SchoolClass> findClassesByTeacherEmail(String schoolId, String email) {
        return teacherRepository.findByEmailAndSchoolId(email, schoolId).map(teacher -> {
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

    // ── Class enrolments (student → class) ───────────────────────
    public List<ClassEnrolment> getClassEnrolments(String classId, String schoolId) {
        return enrolmentRepository.findByClassIdAndSchoolId(classId, schoolId);
    }

    public ClassEnrolment enrolStudent(String classId, String schoolId, ClassEnrolment dto) {
        String year = dto.getAcademicYear() != null ? dto.getAcademicYear() : String.valueOf(java.time.Year.now().getValue());
        if (enrolmentRepository.existsByClassIdAndStudentIdAndAcademicYear(classId, dto.getStudentId(), year)) {
            throw new IllegalArgumentException("Student is already enrolled in this class for " + year);
        }
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
        return departmentRepository.save(dto);
    }

    public Department updateDepartment(String id, String schoolId, Department dto) {
        Department d = findDepartmentById(id, schoolId);
        if (dto.getName() != null) d.setName(dto.getName());
        if (dto.getCode() != null) d.setCode(dto.getCode());
        if (dto.getDescription() != null) d.setDescription(dto.getDescription());
        if (dto.getHead() != null) d.setHead(dto.getHead());
        return departmentRepository.save(d);
    }

    public void deleteDepartment(String id, String schoolId) {
        Department d = findDepartmentById(id, schoolId);
        d.setActive(false);
        departmentRepository.save(d);
    }
}
