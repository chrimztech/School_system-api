package com.srms.api.modules.assessment.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.entity.Department;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.academic.repository.DepartmentRepository;
import com.srms.api.modules.academic.repository.SubjectRepository;
import com.srms.api.modules.academic.repository.TeacherClassSubjectRepository;
import com.srms.api.modules.assessment.dto.GradingBandDto;
import com.srms.api.modules.assessment.entity.Assessment;
import com.srms.api.modules.assessment.entity.AssessmentResult;
import com.srms.api.modules.assessment.entity.PublishedTermGrade;
import com.srms.api.modules.assessment.repository.AssessmentRepository;
import com.srms.api.modules.assessment.repository.ResultRepository;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final ResultRepository resultRepository;
    private final TermGradeService termGradeService;
    private final GradingScaleService gradingScaleService;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassSubjectRepository teacherSubjectRepository;
    private final ClassEnrolmentRepository classEnrolmentRepository;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;

    private static final Set<String> FULL_ACCESS_ROLES = Set.of(
            "SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD");

    public List<Assessment> findAll(String schoolId) {
        return assessmentRepository.findBySchoolIdOrderByDateDesc(schoolId);
    }

    public List<Assessment> findAll(String schoolId, String term, String academicYear) {
        if (term == null && academicYear == null) return findAll(schoolId);
        return assessmentRepository.findBySchoolIdAndTermAndAcademicYear(schoolId, term, academicYear);
    }

    public List<Assessment> findAllForActor(String schoolId, String term, String academicYear,
                                            String userId, String role) {
        assertActorSchool(schoolId, userId, role);
        String normalRole = role == null ? "" : role.toUpperCase();
        if (!FULL_ACCESS_ROLES.contains(normalRole)
                && !"CAREER_GUIDANCE".equals(normalRole)
                && !"TEACHER".equals(normalRole)
                && !"HOD".equals(normalRole)) {
            throw new ForbiddenException("Your role cannot access assessment operations");
        }
        List<Assessment> all = findAll(schoolId, term, academicYear);
        if (FULL_ACCESS_ROLES.contains(normalRole) || "CAREER_GUIDANCE".equals(normalRole)) return all;
        if ("TEACHER".equals(normalRole)) {
            Set<String> assignments = teacherAssignmentKeys(schoolId, userId);
            return all.stream()
                    .filter(a -> assignments.contains(assignmentKey(a.getClassName(), a.getSubjectName()))
                            || assignments.contains(assignmentKey(a.getClassId(), a.getSubjectName())))
                    .toList();
        }
        if ("HOD".equals(normalRole)) {
            Set<String> subjects = hodSubjectNames(schoolId, userId, role);
            return all.stream()
                    .filter(a -> a.getSubjectName() != null
                            && subjects.contains(a.getSubjectName().trim().toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    public Assessment findById(String id, String schoolId) {
        return assessmentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
    }

    public Assessment findByIdForActor(String id, String schoolId, String userId, String role) {
        assertActorSchool(schoolId, userId, role);
        String normalRole = role == null ? "" : role.toUpperCase();
        if (!FULL_ACCESS_ROLES.contains(normalRole)
                && !"CAREER_GUIDANCE".equals(normalRole)
                && !"TEACHER".equals(normalRole)
                && !"HOD".equals(normalRole)) {
            throw new ForbiddenException("Your role cannot access assessment operations");
        }
        Assessment assessment = findById(id, schoolId);
        if (FULL_ACCESS_ROLES.contains(normalRole) || "CAREER_GUIDANCE".equals(normalRole)) {
            return assessment;
        }
        if ("TEACHER".equals(normalRole)) {
            Set<String> assignments = teacherAssignmentKeys(schoolId, userId);
            boolean assigned = assignments.contains(
                    assignmentKey(assessment.getClassName(), assessment.getSubjectName()))
                    || assignments.contains(
                    assignmentKey(assessment.getClassId(), assessment.getSubjectName()));
            if (!assigned) throw new ForbiddenException("You are not assigned to this class/subject");
            return assessment;
        }
        if ("HOD".equals(normalRole)) {
            requireHodOverDepartment(schoolId, userId, role, assessment.getSubjectName());
            return assessment;
        }
        throw new ForbiddenException("Your role cannot access this assessment");
    }

    public Assessment create(String schoolId, Assessment dto, String userId, String role) {
        assertCanManage(schoolId, userId, role, dto.getClassName() != null ? dto.getClassName() : dto.getClassId(),
                dto.getSubjectName());
        dto.setSchoolId(schoolId);
        dto.setTeacherId(userId);
        dto.setPublished(false);
        dto.setWorkflowStatus(Assessment.WorkflowStatus.DRAFT);
        dto.setReportingPeriod(normalisePeriod(dto, schoolId));
        return assessmentRepository.save(dto);
    }

    public Assessment update(String id, String schoolId, Assessment dto, String userId, String role) {
        Assessment assessment = findById(id, schoolId);
        assertCanManage(schoolId, userId, role, assessmentClass(assessment), assessment.getSubjectName());
        assertEditable(assessment);
        if (dto.getTitle() != null) assessment.setTitle(dto.getTitle());
        if (dto.getMaxScore() > 0) assessment.setMaxScore(dto.getMaxScore());
        if (dto.getWeight() > 0) assessment.setWeight(dto.getWeight());
        if (dto.getSubjectName() != null) assessment.setSubjectName(dto.getSubjectName());
        if (dto.getSubjectId() != null) assessment.setSubjectId(dto.getSubjectId());
        if (dto.getTerm() != null) assessment.setTerm(dto.getTerm());
        if (dto.getAcademicYear() != null) assessment.setAcademicYear(dto.getAcademicYear());
        if (dto.getReportingPeriod() != null) assessment.setReportingPeriod(normalisePeriod(dto, schoolId));
        return assessmentRepository.save(assessment);
    }

    public void delete(String id, String schoolId, String userId, String role) {
        Assessment assessment = findById(id, schoolId);
        assertCanManage(schoolId, userId, role, assessmentClass(assessment), assessment.getSubjectName());
        assertEditable(assessment);
        resultRepository.deleteByAssessmentId(id);
        assessmentRepository.delete(assessment);
    }

    public List<AssessmentResult> getResults(String assessmentId, String schoolId, String userId, String role) {
        findByIdForActor(assessmentId, schoolId, userId, role);
        return resultRepository.findByAssessmentId(assessmentId);
    }

    public AssessmentResult saveResult(AssessmentResult result, String userId, String role) {
        Assessment assessment = findById(result.getAssessmentId(), result.getSchoolId());
        assertCanManage(result.getSchoolId(), userId, role, assessmentClass(assessment), assessment.getSubjectName());
        assertEditable(assessment);
        List<ClassEnrolment> roster = activeRoster(assessment);
        if (roster.stream().noneMatch(enrolment -> enrolment.getStudentId().equals(result.getStudentId()))) {
            throw new BusinessException("The learner is not actively enrolled in this class");
        }
        AssessmentResult target = resultRepository
                .findByAssessmentIdAndStudentId(assessment.getId(), result.getStudentId())
                .orElseGet(AssessmentResult::new);
        target.setSchoolId(assessment.getSchoolId());
        target.setAssessmentId(assessment.getId());
        target.setStudentId(result.getStudentId());
        target.setStudentName(result.getStudentName());
        target.setScore(result.getScore());
        target.setAbsent(result.isAbsent());
        gradeResult(target, assessment);
        AssessmentResult saved = resultRepository.save(target);
        assessment.setSubmitted(resultRepository.findByAssessmentId(assessment.getId()).size());
        assessment.setTotal(roster.size());
        assessmentRepository.save(assessment);
        recomputeProvisional(assessment);
        return saved;
    }

    public List<AssessmentResult> saveResultsBulk(String assessmentId, String schoolId,
                                                  List<AssessmentResult> results, String userId, String role) {
        Assessment assessment = findById(assessmentId, schoolId);
        assertCanManage(schoolId, userId, role, assessmentClass(assessment), assessment.getSubjectName());
        assertEditable(assessment);

        List<ClassEnrolment> roster = activeRoster(assessment);
        Set<String> rosterIds = roster.stream().map(ClassEnrolment::getStudentId).collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        List<AssessmentResult> clean = new ArrayList<>();
        List<GradingBandDto> gradingBands = gradingScaleService.getBands(schoolId);
        for (AssessmentResult result : results) {
            if (!rosterIds.contains(result.getStudentId())) {
                throw new BusinessException("A result was supplied for a learner not enrolled in this class");
            }
            if (!seen.add(result.getStudentId())) {
                throw new BusinessException("A learner cannot have duplicate results for one assessment");
            }
            result.setAssessmentId(assessmentId);
            result.setSchoolId(schoolId);
            gradeResult(result, assessment, gradingBands);
            clean.add(result);
        }

        resultRepository.deleteByAssessmentId(assessmentId);
        List<AssessmentResult> saved = resultRepository.saveAll(clean);
        assessment.setSubmitted(saved.size());
        assessment.setTotal(roster.size());
        assessment.setWorkflowStatus(Assessment.WorkflowStatus.DRAFT);
        assessment.setPublished(false);
        assessment.setSubmittedAt(null);
        assessment.setSubmittedBy(null);
        assessment.setVerifiedAt(null);
        assessment.setVerifiedBy(null);
        assessment.setPublishedAt(null);
        assessment.setPublishedBy(null);
        assessment.setReviewNote(null);
        assessmentRepository.save(assessment);
        recomputeProvisional(assessment);
        return saved;
    }

    public Assessment submitForVerification(String assessmentId, String schoolId, String userId, String role) {
        Assessment assessment = findById(assessmentId, schoolId);
        assertCanManage(schoolId, userId, role, assessmentClass(assessment), assessment.getSubjectName());
        assertEditable(assessment);
        assertComplete(assessment);
        assessment.setWorkflowStatus(Assessment.WorkflowStatus.SUBMITTED);
        assessment.setSubmittedBy(userId);
        assessment.setSubmittedAt(LocalDateTime.now());
        assessment.setReviewNote(null);
        return assessmentRepository.save(assessment);
    }

    public Assessment verify(String assessmentId, String schoolId, String userId, String role) {
        Assessment assessment = findById(assessmentId, schoolId);
        requireHodOverDepartment(schoolId, userId, role, assessment.getSubjectName());
        if (assessment.getWorkflowStatus() != Assessment.WorkflowStatus.SUBMITTED) {
            throw new BusinessException("Only submitted results can be verified");
        }
        assertComplete(assessment);
        assessment.setWorkflowStatus(Assessment.WorkflowStatus.VERIFIED);
        assessment.setVerifiedBy(userId);
        assessment.setVerifiedAt(LocalDateTime.now());
        assessment.setReviewNote(null);
        return assessmentRepository.save(assessment);
    }

    public Assessment reject(String assessmentId, String schoolId, String note, String userId, String role) {
        Assessment assessment = findById(assessmentId, schoolId);
        requireHodOverDepartment(schoolId, userId, role, assessment.getSubjectName());
        if (assessment.getWorkflowStatus() != Assessment.WorkflowStatus.SUBMITTED) {
            throw new BusinessException("Only submitted results can be returned");
        }
        if (note == null || note.isBlank()) throw new BusinessException("A correction note is required");
        assessment.setWorkflowStatus(Assessment.WorkflowStatus.REJECTED);
        assessment.setReviewNote(note.trim());
        assessment.setVerifiedBy(null);
        assessment.setVerifiedAt(null);
        return assessmentRepository.save(assessment);
    }

    /** HOD verification is scoped to their own department: the assessment's subject must
     * belong to a Subject whose `department` name matches a Department this HOD actually
     * heads (Department.headTeacherId -> Teacher -> email -> this AppUser). Admin-tier roles
     * bypass the check entirely. */
    private void requireHodOverDepartment(String schoolId, String userId, String role, String subjectName) {
        assertActorSchool(schoolId, userId, role);
        String normalRole = role == null ? "" : role.toUpperCase();
        if (FULL_ACCESS_ROLES.contains(normalRole)) return;
        if (!"HOD".equals(normalRole)) {
            throw new ForbiddenException("Only a Head of Department can verify or return results");
        }
        Set<String> subjects = hodSubjectNames(schoolId, userId, role);
        if (subjectName == null || !subjects.contains(subjectName.trim().toLowerCase())) {
            throw new ForbiddenException("This subject is not in your department");
        }
    }

    private Set<String> hodSubjectNames(String schoolId, String userId, String role) {
        assertActorSchool(schoolId, userId, role);
        if (!"HOD".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only a Head of Department can access this review queue");
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) {
            throw new ForbiddenException("Could not resolve your staff record");
        }
        Teacher teacher = teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId).orElse(null);
        if (teacher == null) {
            throw new ForbiddenException("Could not resolve your staff record");
        }
        Department department = departmentRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(d -> teacher.getId().equals(d.getHeadTeacherId()))
                .findFirst().orElse(null);
        if (department == null) {
            throw new ForbiddenException("You are not the head of a department");
        }
        return subjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(s -> department.getName() != null
                        && department.getName().equalsIgnoreCase(s.getDepartment()))
                .map(s -> s.getName() == null ? "" : s.getName().trim().toLowerCase())
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
    }

    public Map<String, Object> publishCycle(String assessmentId, String schoolId, String userId, String role) {
        assertRoleAndSchool("CAREER_GUIDANCE", schoolId, userId, role,
                "Only Careers Guidance can publish results");
        Assessment anchor = findById(assessmentId, schoolId);
        if (anchor.getWorkflowStatus() != Assessment.WorkflowStatus.VERIFIED) {
            throw new BusinessException("Results must be verified by an HOD before publication");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", schoolId));
        String mode = school.getResultPublicationMode() == null ? "SEPARATE" : school.getResultPublicationMode();
        Assessment.ReportingPeriod period = "COMBINED".equalsIgnoreCase(mode)
                ? Assessment.ReportingPeriod.COMBINED
                : termGradeService.effectivePeriod(anchor, mode);

        String canonicalClassId = termGradeService.resolveClassId(schoolId, assessmentClassReference(anchor));
        List<Assessment> allForTerm = assessmentRepository
                .findBySchoolIdAndTermAndAcademicYear(schoolId, anchor.getTerm(), anchor.getAcademicYear()).stream()
                .filter(assessment -> belongsToClass(assessment, schoolId, canonicalClassId))
                .toList();
        List<Assessment> releaseGroup = allForTerm.stream()
                .filter(a -> "COMBINED".equalsIgnoreCase(mode)
                        || termGradeService.effectivePeriod(a, mode) == period)
                .toList();
        if (releaseGroup.isEmpty()) throw new BusinessException("No assessments were found for this result cycle");
        for (Assessment assessment : releaseGroup) {
            if (assessment.getWorkflowStatus() != Assessment.WorkflowStatus.VERIFIED
                    && assessment.getWorkflowStatus() != Assessment.WorkflowStatus.PUBLISHED) {
                throw new BusinessException("All results in this class and reporting period must be entered and HOD-verified before publication");
            }
            assertComplete(assessment);
        }

        Set<String> subjects = allForTerm.stream()
                .filter(a -> period != Assessment.ReportingPeriod.MIDTERM
                        || termGradeService.effectivePeriod(a, mode) == Assessment.ReportingPeriod.MIDTERM)
                .map(Assessment::getSubjectName).filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        List<PublishedTermGrade> snapshots = termGradeService.publishSnapshots(
                schoolId, anchor.getClassId(), anchor.getTerm(), anchor.getAcademicYear(),
                period, subjects, userId);

        LocalDateTime now = LocalDateTime.now();
        releaseGroup.forEach(assessment -> {
            assessment.setWorkflowStatus(Assessment.WorkflowStatus.PUBLISHED);
            assessment.setPublished(true);
            assessment.setPublishedBy(userId);
            assessment.setPublishedAt(now);
        });
        assessmentRepository.saveAll(releaseGroup);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reportingPeriod", period.name());
        response.put("assessmentsPublished", releaseGroup.size());
        response.put("reportCardRowsPublished", snapshots.size());
        return response;
    }

    private void gradeResult(AssessmentResult result, Assessment assessment) {
        gradeResult(result, assessment, gradingScaleService.getBands(assessment.getSchoolId()));
    }

    private void gradeResult(AssessmentResult result, Assessment assessment, List<GradingBandDto> gradingBands) {
        if (result.isAbsent()) {
            result.setScore(null);
            result.setGrade("X");
            result.setRemarks("ABSENT");
            return;
        }
        if (result.getScore() == null) throw new BusinessException("A score or absent status is required");
        if (result.getScore() < 0 || result.getScore() > assessment.getMaxScore()) {
            throw new BusinessException("Scores must be between 0 and " + assessment.getMaxScore());
        }
        double percentage = assessment.getMaxScore() > 0
                ? result.getScore() / assessment.getMaxScore() * 100 : 0;
        GradingBandDto band = gradingScaleService.evaluate(gradingBands, percentage);
        result.setGrade(band.getGrade());
        result.setRemarks(band.getDescription());
    }

    private void assertComplete(Assessment assessment) {
        List<ClassEnrolment> roster = activeRoster(assessment);
        List<AssessmentResult> results = resultRepository.findByAssessmentId(assessment.getId());
        Set<String> learnerIds = results.stream().map(AssessmentResult::getStudentId).collect(Collectors.toSet());
        boolean everyResultValid = results.stream().allMatch(r -> r.isAbsent() || r.getScore() != null);
        if (roster.isEmpty() || results.size() != roster.size() || learnerIds.size() != roster.size() || !everyResultValid
                || !learnerIds.containsAll(roster.stream().map(ClassEnrolment::getStudentId).toList())) {
            throw new BusinessException("Enter a score or mark absent for every enrolled learner before submission");
        }
        assessment.setSubmitted(results.size());
        assessment.setTotal(roster.size());
    }

    private List<ClassEnrolment> activeRoster(Assessment assessment) {
        String classId = termGradeService.resolveClassId(
                assessment.getSchoolId(), assessmentClassReference(assessment));
        return classEnrolmentRepository.findByClassIdAndSchoolId(classId, assessment.getSchoolId()).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> e.getAcademicYear() == null || e.getAcademicYear().equals(assessment.getAcademicYear()))
                .toList();
    }

    private void recomputeProvisional(Assessment assessment) {
        if (assessment.getClassId() == null || assessment.getSubjectName() == null
                || assessment.getTerm() == null || assessment.getAcademicYear() == null) return;
        try {
            termGradeService.compute(assessment.getSchoolId(), assessment.getClassId(), assessment.getSubjectName(),
                    assessment.getTerm(), assessment.getAcademicYear());
        } catch (Exception exception) {
            log.warn("Term grade auto-compute failed for assessment {}: {}", assessment.getId(), exception.getMessage());
        }
    }

    private Assessment.ReportingPeriod normalisePeriod(Assessment assessment, String schoolId) {
        String mode = schoolRepository.findById(schoolId).map(School::getResultPublicationMode).orElse("SEPARATE");
        if ("COMBINED".equalsIgnoreCase(mode)) return Assessment.ReportingPeriod.COMBINED;
        if (assessment.getReportingPeriod() == Assessment.ReportingPeriod.MIDTERM
                || assessment.getReportingPeriod() == Assessment.ReportingPeriod.END_TERM) {
            return assessment.getReportingPeriod();
        }
        return assessment.getType() == Assessment.AssessmentType.midterm
                ? Assessment.ReportingPeriod.MIDTERM : Assessment.ReportingPeriod.END_TERM;
    }

    private void assertEditable(Assessment assessment) {
        Assessment.WorkflowStatus status = assessment.getWorkflowStatus() == null
                ? Assessment.WorkflowStatus.DRAFT : assessment.getWorkflowStatus();
        if (status != Assessment.WorkflowStatus.DRAFT && status != Assessment.WorkflowStatus.REJECTED) {
            throw new BusinessException("Results cannot be edited while they are awaiting verification, verified, or published");
        }
    }

    private String assessmentClass(Assessment assessment) {
        return assessment.getClassName() != null ? assessment.getClassName() : assessment.getClassId();
    }

    private String assessmentClassReference(Assessment assessment) {
        return assessment.getClassId() != null ? assessment.getClassId() : assessment.getClassName();
    }

    private boolean belongsToClass(Assessment assessment, String schoolId, String canonicalClassId) {
        String reference = assessmentClassReference(assessment);
        if (reference == null || reference.isBlank()) return false;
        try {
            return canonicalClassId.equals(termGradeService.resolveClassId(schoolId, reference));
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private void assertCanManage(String schoolId, String userId, String role,
                                 String className, String subjectName) {
        assertActorSchool(schoolId, userId, role);
        String normalRole = role == null ? "" : role.toUpperCase();
        if (FULL_ACCESS_ROLES.contains(normalRole)) return;
        if (!"TEACHER".equals(normalRole)) {
            throw new ForbiddenException("Your role does not have permission to manage assessments");
        }
        Set<String> assignments = teacherAssignmentKeys(schoolId, userId);
        if (!assignments.contains(assignmentKey(className, subjectName))) {
            throw new ForbiddenException("You are not assigned to teach this class/subject");
        }
    }

    private void assertRoleAndSchool(String requiredRole, String schoolId, String userId,
                                     String actualRole, String message) {
        assertActorSchool(schoolId, userId, actualRole);
        if (!requiredRole.equalsIgnoreCase(actualRole)) throw new ForbiddenException(message);
    }

    private void assertActorSchool(String schoolId, String userId, String role) {
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) return;
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Authenticated user was not found"));
        if (user.getSchoolId() == null || !user.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("You cannot access another school's assessment records");
        }
    }

    private String assignmentKey(String className, String subjectName) {
        return (className == null ? "" : className.trim().toLowerCase()) + "::"
                + (subjectName == null ? "" : subjectName.trim().toLowerCase());
    }

    private Set<String> teacherAssignmentKeys(String schoolId, String userId) {
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return Set.of();
        Teacher teacher = teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId).orElse(null);
        if (teacher == null) return Set.of();
        return teacherSubjectRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .map(t -> assignmentKey(t.getClassName(), t.getSubjectName()))
                .collect(Collectors.toSet());
    }

    public List<AssessmentResult> getStudentResults(String schoolId, String studentId, boolean publishedOnly,
                                                     String userId, String role) {
        assertActorSchool(schoolId, userId, role);
        List<AssessmentResult> results = resultRepository.findBySchoolIdAndStudentId(schoolId, studentId);
        if (!publishedOnly) return results;
        Set<String> publishedIds = assessmentRepository.findBySchoolIdOrderByDateDesc(schoolId).stream()
                .filter(a -> a.getWorkflowStatus() == Assessment.WorkflowStatus.PUBLISHED)
                .map(Assessment::getId).collect(Collectors.toSet());
        return results.stream().filter(r -> publishedIds.contains(r.getAssessmentId())).toList();
    }

    public Page<AssessmentResult> getStudentResultsPaged(String schoolId, String studentId, Pageable pageable,
                                                          String userId, String role) {
        assertActorSchool(schoolId, userId, role);
        return resultRepository.findBySchoolIdAndStudentId(schoolId, studentId, pageable);
    }

    public List<Map<String, Object>> getEnrichedStudentResults(String schoolId, String studentId,
                                                                boolean publishedOnly, String userId, String role) {
        return getStudentResults(schoolId, studentId, publishedOnly, userId, role).stream().map(result -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", result.getId());
            map.put("assessmentId", result.getAssessmentId());
            map.put("score", result.getScore());
            map.put("grade", result.getGrade());
            map.put("remarks", result.getRemarks());
            map.put("absent", result.isAbsent());
            assessmentRepository.findByIdAndSchoolId(result.getAssessmentId(), schoolId).ifPresent(assessment -> {
                map.put("title", assessment.getTitle());
                map.put("subjectName", assessment.getSubjectName());
                map.put("type", assessment.getType() != null ? assessment.getType().name() : null);
                map.put("maxScore", assessment.getMaxScore());
                map.put("date", assessment.getDate() != null ? assessment.getDate().toString() : null);
                map.put("weight", assessment.getWeight());
                map.put("published", assessment.getWorkflowStatus() == Assessment.WorkflowStatus.PUBLISHED);
            });
            return map;
        }).toList();
    }
}
