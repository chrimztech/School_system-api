package com.srms.api.modules.exam.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.ClassEnrolment;
import com.srms.api.modules.academic.repository.ClassEnrolmentRepository;
import com.srms.api.modules.exam.entity.ExamCandidate;
import com.srms.api.modules.exam.entity.ExamPaper;
import com.srms.api.modules.exam.entity.GceCandidate;
import com.srms.api.modules.exam.repository.ExamCandidateRepository;
import com.srms.api.modules.exam.repository.ExamRepository;
import com.srms.api.modules.exam.repository.GceCandidateRepository;
import com.srms.api.modules.student.entity.Student;
import com.srms.api.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamCandidateService {
    private final ExamCandidateRepository repo;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final GceCandidateRepository gceCandidateRepository;
    private final ClassEnrolmentRepository classEnrolmentRepository;

    public List<ExamCandidate> list(String schoolId, String examPaperId) {
        findPaper(schoolId, examPaperId);
        return repo.findByExamPaperIdAndSchoolId(examPaperId, schoolId);
    }

    public ExamCandidate addStudent(String schoolId, String examPaperId, String studentId) {
        ExamPaper paper = findPaper(schoolId, examPaperId);
        if (repo.existsByExamPaperIdAndStudentId(examPaperId, studentId)) {
            throw new IllegalArgumentException("Student is already registered for this paper");
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        ExamCandidate candidate = ExamCandidate.builder()
                .schoolId(schoolId)
                .examPaperId(paper.getId())
                .candidateType("INTERNAL")
                .studentId(student.getId())
                .candidateName(fullName(student.getFirstName(), student.getLastName()))
                .grade(String.valueOf(student.getGrade()))
                .status("REGISTERED")
                .build();
        ExamCandidate saved = repo.save(candidate);
        syncCount(paper);
        return saved;
    }

    public Map<String, Integer> addFromClass(String schoolId, String examPaperId, String classId) {
        ExamPaper paper = findPaper(schoolId, examPaperId);
        List<ClassEnrolment> enrolments = classEnrolmentRepository.findByClassIdAndSchoolId(classId, schoolId);
        int added = 0;
        int skipped = 0;
        for (ClassEnrolment enrolment : enrolments) {
            if (!"ACTIVE".equals(enrolment.getStatus())) { skipped++; continue; }
            if (repo.existsByExamPaperIdAndStudentId(examPaperId, enrolment.getStudentId())) { skipped++; continue; }
            ExamCandidate candidate = ExamCandidate.builder()
                    .schoolId(schoolId)
                    .examPaperId(paper.getId())
                    .candidateType("INTERNAL")
                    .studentId(enrolment.getStudentId())
                    .candidateName(enrolment.getStudentName())
                    .grade(enrolment.getGrade())
                    .status("REGISTERED")
                    .build();
            repo.save(candidate);
            added++;
        }
        syncCount(paper);
        return Map.of("added", added, "skipped", skipped);
    }

    public Map<String, Integer> addGceCandidates(String schoolId, String examPaperId, List<String> gceCandidateIds) {
        ExamPaper paper = findPaper(schoolId, examPaperId);
        int added = 0;
        int skipped = 0;
        for (String gceCandidateId : gceCandidateIds) {
            if (repo.existsByExamPaperIdAndGceCandidateId(examPaperId, gceCandidateId)) { skipped++; continue; }
            GceCandidate gce = gceCandidateRepository.findByIdAndSchoolId(gceCandidateId, schoolId).orElse(null);
            if (gce == null) { skipped++; continue; }
            ExamCandidate candidate = ExamCandidate.builder()
                    .schoolId(schoolId)
                    .examPaperId(paper.getId())
                    .candidateType("GCE")
                    .gceCandidateId(gce.getId())
                    .candidateName(fullName(gce.getFirstName(), gce.getLastName()))
                    .grade(gce.getGrade())
                    .status("REGISTERED")
                    .build();
            repo.save(candidate);
            added++;
        }
        syncCount(paper);
        return Map.of("added", added, "skipped", skipped);
    }

    public void remove(String schoolId, String examPaperId, String candidateId) {
        ExamPaper paper = findPaper(schoolId, examPaperId);
        ExamCandidate candidate = repo.findByIdAndSchoolId(candidateId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamCandidate", candidateId));
        repo.delete(candidate);
        syncCount(paper);
    }

    private ExamPaper findPaper(String schoolId, String examPaperId) {
        return examRepository.findById(examPaperId)
                .filter(p -> p.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("ExamPaper", examPaperId));
    }

    private void syncCount(ExamPaper paper) {
        paper.setCandidates((int) repo.countByExamPaperId(paper.getId()));
        examRepository.save(paper);
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
