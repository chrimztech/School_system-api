package com.srms.api.modules.curriculum.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.academic.entity.SchoolClass;
import com.srms.api.modules.academic.repository.SchoolClassRepository;
import com.srms.api.modules.curriculum.dto.SchemeRequests;
import com.srms.api.modules.curriculum.entity.CurriculumTopic;
import com.srms.api.modules.curriculum.entity.SchemeOfWork;
import com.srms.api.modules.curriculum.entity.SchemeWeek;
import com.srms.api.modules.curriculum.repository.CurriculumTopicRepository;
import com.srms.api.modules.curriculum.repository.SchemeOfWorkRepository;
import com.srms.api.modules.curriculum.repository.SchemeWeekRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheme of work lifecycle: a teacher drafts it (or populates it from the curriculum topics),
 * submits it, and a reviewer approves or returns it. Only drafts and returned schemes are editable,
 * so an approved scheme is a stable record that lesson plans can be generated from.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SchemeService {
    private final SchemeOfWorkRepository schemes;
    private final SchemeWeekRepository weeks;
    private final CurriculumTopicRepository topics;
    private final SchoolClassRepository classes;

    public List<SchemeOfWork> list(String schoolId) {
        return schemes.findBySchoolIdOrderByCreatedAtDesc(schoolId);
    }

    public SchemeOfWork get(String schoolId, String id) {
        return schemes.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme of work", id));
    }

    public List<SchemeWeek> weeks(String schoolId, String schemeId) {
        get(schoolId, schemeId);
        return weeks.findBySchemeIdOrderByWeekNumberAsc(schemeId);
    }

    public SchemeOfWork create(String schoolId, SchemeRequests.Create req, String actor) {
        if (blank(req.classId()) || blank(req.subjectName()) || blank(req.term()) || blank(req.academicYear())) {
            throw new BusinessException("Class, subject, term and academic year are all required");
        }
        SchoolClass cls = classes.findByIdAndSchoolId(req.classId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", req.classId()));
        SchemeOfWork scheme = new SchemeOfWork();
        scheme.setSchoolId(schoolId);
        scheme.setClassId(cls.getId());
        scheme.setClassName(cls.getName());
        scheme.setSubjectName(req.subjectName().trim());
        scheme.setTerm(req.term().trim());
        scheme.setAcademicYear(req.academicYear().trim());
        scheme.setStatus(SchemeOfWork.Status.DRAFT);
        scheme.setCreatedBy(actor);
        return schemes.save(scheme);
    }

    public List<SchemeWeek> replaceWeeks(String schoolId, String id, List<SchemeRequests.WeekInput> inputs, String actor, boolean reviewer) {
        SchemeOfWork scheme = editable(schoolId, id, actor, reviewer);
        weeks.deleteBySchemeId(scheme.getId());
        List<SchemeWeek> rows = new ArrayList<>();
        int number = 1;
        for (SchemeRequests.WeekInput in : inputs == null ? List.<SchemeRequests.WeekInput>of() : inputs) {
            if (blank(in.topic())) continue;
            SchemeWeek w = new SchemeWeek();
            w.setSchoolId(schoolId);
            w.setSchemeId(scheme.getId());
            w.setWeekNumber(number++);
            w.setTopic(in.topic().trim());
            w.setSubTopics(in.subTopics());
            w.setObjectives(in.objectives());
            w.setActivities(in.activities());
            w.setResources(in.resources());
            w.setAssessment(in.assessment());
            w.setRemarks(in.remarks());
            rows.add(w);
        }
        return weeks.saveAll(rows);
    }

    /** Fills the scheme from the syllabus topics already entered for this subject, grade and term. */
    public List<SchemeWeek> populateFromCurriculum(String schoolId, String id, String actor, boolean reviewer) {
        SchemeOfWork scheme = editable(schoolId, id, actor, reviewer);
        SchoolClass cls = classes.findByIdAndSchoolId(scheme.getClassId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", scheme.getClassId()));
        List<CurriculumTopic> source = topics.findBySchoolIdAndSubjectNameAndGradeAndTermOrderBySequenceAsc(
                schoolId, scheme.getSubjectName(), cls.getGrade(), scheme.getTerm());
        if (source.isEmpty()) {
            throw new BusinessException("No syllabus topics are entered yet for "
                    + scheme.getSubjectName() + ", Grade " + cls.getGrade() + ", Term " + scheme.getTerm()
                    + " — add them on the Syllabus & topics tab first");
        }
        List<SchemeRequests.WeekInput> inputs = source.stream()
                .map(t -> new SchemeRequests.WeekInput(t.getTopic(), t.getSubTopics(), t.getObjectives(),
                        t.getActivities(), t.getResources(), t.getAssessment(), null))
                .toList();
        return replaceWeeks(schoolId, id, inputs, actor, reviewer);
    }

    public SchemeOfWork submit(String schoolId, String id, String actor) {
        SchemeOfWork scheme = get(schoolId, id);
        if (!actor.equals(scheme.getCreatedBy())) throw new ForbiddenException("Only the teacher who wrote this scheme can submit it");
        if (scheme.getStatus() != SchemeOfWork.Status.DRAFT && scheme.getStatus() != SchemeOfWork.Status.REJECTED) {
            throw new BusinessException("Only a draft or a returned scheme can be submitted");
        }
        if (weeks.findBySchemeIdOrderByWeekNumberAsc(id).isEmpty()) {
            throw new BusinessException("Add at least one week before submitting");
        }
        scheme.setStatus(SchemeOfWork.Status.SUBMITTED);
        scheme.setSubmittedAt(LocalDateTime.now());
        scheme.setReviewNote(null);
        return schemes.save(scheme);
    }

    public SchemeOfWork review(String schoolId, String id, SchemeRequests.Review req, String actor) {
        SchemeOfWork scheme = get(schoolId, id);
        if (scheme.getStatus() != SchemeOfWork.Status.SUBMITTED) {
            throw new BusinessException("Only a submitted scheme can be reviewed");
        }
        boolean approve = Boolean.TRUE.equals(req.approve());
        if (!approve && blank(req.note())) {
            throw new BusinessException("Add a note explaining what the teacher should change");
        }
        scheme.setStatus(approve ? SchemeOfWork.Status.APPROVED : SchemeOfWork.Status.REJECTED);
        scheme.setReviewedBy(actor);
        scheme.setReviewedAt(LocalDateTime.now());
        scheme.setReviewNote(blank(req.note()) ? null : req.note().trim());
        return schemes.save(scheme);
    }

    private SchemeOfWork editable(String schoolId, String id, String actor, boolean reviewer) {
        SchemeOfWork scheme = get(schoolId, id);
        if (!reviewer && !actor.equals(scheme.getCreatedBy())) {
            throw new ForbiddenException("Only the teacher who wrote this scheme can change it");
        }
        if (scheme.getStatus() != SchemeOfWork.Status.DRAFT && scheme.getStatus() != SchemeOfWork.Status.REJECTED) {
            throw new BusinessException("This scheme is " + scheme.getStatus().name().toLowerCase()
                    + " and can no longer be edited");
        }
        return scheme;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
