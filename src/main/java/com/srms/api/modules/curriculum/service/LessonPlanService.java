package com.srms.api.modules.curriculum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.curriculum.dto.LessonPlanRequests;
import com.srms.api.modules.curriculum.entity.LessonPlan;
import com.srms.api.modules.curriculum.entity.SchemeOfWork;
import com.srms.api.modules.curriculum.entity.SchemeWeek;
import com.srms.api.modules.curriculum.repository.LessonPlanRepository;
import com.srms.api.modules.curriculum.repository.SchemeOfWorkRepository;
import com.srms.api.modules.curriculum.repository.SchemeWeekRepository;
import com.srms.api.modules.integration.client.AnthropicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonPlanService {
    private static final int DEFAULT_DURATION = 40;
    private static final String SYSTEM_PROMPT = """
            You write lesson plans for teachers in Zambian schools, following the Zambian lesson plan
            structure: introduction, development (teacher and learner activities in clear steps),
            conclusion, evaluation and homework. Use simple, practical language and examples that
            fit a Zambian classroom. Only refer to textbooks or materials if they are named in the
            scheme. Respond with a single JSON object and nothing else, using exactly these keys:
            "topic", "objectives" (array of strings), "materials" (array of strings), "introduction",
            "development", "conclusion", "evaluation", "homework".
            """;

    private final LessonPlanRepository lessonPlans;
    private final SchemeOfWorkRepository schemes;
    private final SchemeWeekRepository weeks;
    private final AnthropicClient anthropic;
    private final ObjectMapper objectMapper;

    public List<LessonPlan> list(String schoolId, String classId) {
        return classId == null || classId.isBlank()
                ? lessonPlans.findBySchoolIdOrderByLessonDateDescCreatedAtDesc(schoolId)
                : lessonPlans.findBySchoolIdAndClassIdOrderByLessonDateDesc(schoolId, classId);
    }

    public LessonPlan create(String schoolId, LessonPlanRequests.Save req, String actor) {
        if (req.topic() == null || req.topic().isBlank()) throw new BusinessException("A topic is required");
        if (req.classId() == null || req.classId().isBlank() || req.subjectName() == null || req.subjectName().isBlank()) {
            throw new BusinessException("A class and subject are required");
        }
        LessonPlan plan = new LessonPlan();
        plan.setSchoolId(schoolId);
        plan.setClassId(req.classId());
        plan.setSubjectName(req.subjectName().trim());
        plan.setCreatedBy(actor);
        plan.setSource(LessonPlan.Source.MANUAL);
        applySave(plan, req);
        return lessonPlans.save(plan);
    }

    public LessonPlan update(String schoolId, String id, LessonPlanRequests.Save req, String actor) {
        LessonPlan plan = owned(schoolId, id, actor);
        applySave(plan, req);
        return lessonPlans.save(plan);
    }

    public void delete(String schoolId, String id, String actor) {
        lessonPlans.delete(owned(schoolId, id, actor));
    }

    /**
     * Creates a draft lesson from one week of an approved scheme. TEMPLATE fills the standard
     * sections directly from the week's content; AI asks the school's own Anthropic account to
     * draft them. Either way the result is a DRAFT the teacher reviews before marking it final.
     */
    public LessonPlan generate(String schoolId, LessonPlanRequests.Generate req, String actor) {
        SchemeOfWork scheme = schemes.findByIdAndSchoolId(req.schemeId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme of work", req.schemeId()));
        if (scheme.getStatus() != SchemeOfWork.Status.APPROVED) {
            throw new BusinessException("Lesson plans can only be generated from an approved scheme of work");
        }
        SchemeWeek week = weeks.findByIdAndSchemeId(req.schemeWeekId(), scheme.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme week", req.schemeWeekId()));

        boolean ai = "AI".equalsIgnoreCase(req.mode());
        LessonPlan plan = new LessonPlan();
        plan.setSchoolId(schoolId);
        plan.setSchemeId(scheme.getId());
        plan.setSchemeWeekId(week.getId());
        plan.setClassId(scheme.getClassId());
        plan.setSubjectName(scheme.getSubjectName());
        plan.setCreatedBy(actor);
        plan.setLessonDate(req.lessonDate() == null ? LocalDate.now() : req.lessonDate());
        plan.setDurationMinutes(req.durationMinutes() == null ? DEFAULT_DURATION : req.durationMinutes());
        plan.setStatus(LessonPlan.Status.DRAFT);

        if (ai) {
            Map<String, Object> draft = parseDraft(anthropic.complete(schoolId, SYSTEM_PROMPT, weekPrompt(scheme, week, plan), 2000));
            plan.setSource(LessonPlan.Source.AI);
            plan.setTopic(text(draft.get("topic"), week.getTopic()));
            plan.setObjectives(text(draft.get("objectives"), week.getObjectives()));
            plan.setMaterials(text(draft.get("materials"), week.getResources()));
            plan.setIntroduction(text(draft.get("introduction"), null));
            plan.setDevelopment(text(draft.get("development"), null));
            plan.setConclusion(text(draft.get("conclusion"), null));
            plan.setEvaluation(text(draft.get("evaluation"), null));
            plan.setHomework(text(draft.get("homework"), null));
        } else {
            fillFromTemplate(plan, scheme, week);
        }
        return lessonPlans.save(plan);
    }

    private LessonPlan owned(String schoolId, String id, String actor) {
        LessonPlan plan = lessonPlans.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson plan", id));
        if (!actor.equals(plan.getCreatedBy())) {
            throw new ForbiddenException("Only the teacher who wrote this lesson plan can change it");
        }
        return plan;
    }

    private void applySave(LessonPlan plan, LessonPlanRequests.Save req) {
        if (req.topic() != null) plan.setTopic(req.topic().trim());
        plan.setObjectives(req.objectives());
        plan.setMaterials(req.materials());
        plan.setIntroduction(req.introduction());
        plan.setDevelopment(req.development());
        plan.setConclusion(req.conclusion());
        plan.setEvaluation(req.evaluation());
        plan.setHomework(req.homework());
        if (req.lessonDate() != null) plan.setLessonDate(req.lessonDate());
        if (req.durationMinutes() != null) plan.setDurationMinutes(req.durationMinutes());
        if (req.status() != null) {
            plan.setStatus(LessonPlan.Status.valueOf(req.status().toUpperCase()));
        }
    }

    private static void fillFromTemplate(LessonPlan plan, SchemeOfWork scheme, SchemeWeek week) {
        String topic = week.getTopic();
        plan.setTopic(topic);
        plan.setObjectives(week.getObjectives());
        plan.setMaterials(week.getResources());
        plan.setSource(LessonPlan.Source.TEMPLATE);
        plan.setIntroduction("Revise the previous lesson briefly. Introduce today's topic: " + topic + ".");
        plan.setDevelopment(orDefault(week.getActivities(),
                "Explain " + topic + " with examples from the learners' surroundings. Learners work through the examples in pairs, then share their answers with the class."));
        plan.setConclusion("Summarise the key points of " + topic + " and answer learners' questions.");
        plan.setEvaluation(orDefault(week.getAssessment(),
                "Ask 3 to 5 questions on " + topic + " to check understanding before the lesson ends."));
        plan.setHomework("Complete the exercises on " + topic + " from the set textbook.");
    }

    private String weekPrompt(SchemeOfWork scheme, SchemeWeek week, LessonPlan plan) {
        return """
                Subject: %s
                Class: %s
                Term: %s, %s
                Week %d topic: %s
                Sub-topics: %s
                Objectives from the scheme: %s
                Suggested activities: %s
                Resources: %s
                Assessment: %s
                Lesson date: %s
                Duration: %d minutes

                Write one lesson plan for this lesson.
                """.formatted(
                scheme.getSubjectName(), scheme.getClassName(), scheme.getTerm(), scheme.getAcademicYear(),
                week.getWeekNumber(), week.getTopic(), nz(week.getSubTopics()), nz(week.getObjectives()),
                nz(week.getActivities()), nz(week.getResources()), nz(week.getAssessment()),
                plan.getLessonDate(), plan.getDurationMinutes());
    }

    private Map<String, Object> parseDraft(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("The AI draft could not be read — try generating it again");
        }
        try {
            return objectMapper.readValue(raw.substring(start, end + 1), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("The AI draft could not be read — try generating it again");
        }
    }

    /** Lists become bullet-style lines; anything missing falls back to the given default. */
    private static String text(Object value, String fallback) {
        if (value == null) return fallback;
        if (value instanceof List<?> items) {
            String joined = items.stream().map(String::valueOf).map(s -> "- " + s).reduce((a, b) -> a + "\n" + b).orElse("");
            return joined.isBlank() ? fallback : joined;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "(none given)" : s;
    }

    private static String orDefault(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }
}
