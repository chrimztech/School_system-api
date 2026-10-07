package com.srms.api.modules.curriculum.service;

import com.srms.api.modules.academic.entity.TeacherClassSubject;
import com.srms.api.modules.academic.repository.TeacherClassSubjectRepository;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import com.srms.api.modules.teacher.entity.Teacher;
import com.srms.api.modules.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A teacher may only author a scheme of work (and, by extension, lesson plans) for a class and
 * subject they are personally assigned to teach via TeacherClassSubject — mirrors
 * AssessmentService's own assertCanManage/teacherAssignmentKeys exactly, so "assigned to teach"
 * means the same thing everywhere in the system. HOD and above are exempt (see each call site).
 *
 * Checks both className and classId, same as AssessmentService: an assignment's className is
 * only ever populated when whoever created it happened to send one (ClassController.assignTeacher
 * saves the request body as-is, with no server-side backfill), so a className-only check would
 * silently fail to match a great many real assignments. Callers pass whichever of the two they
 * have for both the asked-about class and each stored assignment; either pairing matching is enough.
 */
@Component
@RequiredArgsConstructor
class TeacherAssignmentLookup {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;

    boolean isAssigned(String schoolId, String userId, String className, String classId, String subjectName) {
        Set<String> keys = assignmentKeys(schoolId, userId);
        return keys.contains(key(className, subjectName)) || keys.contains(key(classId, subjectName));
    }

    private Set<String> assignmentKeys(String schoolId, String userId) {
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return Set.of();
        Teacher teacher = teacherRepository.findByEmailIgnoreCaseAndSchoolId(user.getEmail(), schoolId).orElse(null);
        if (teacher == null) return Set.of();
        List<TeacherClassSubject> rows = teacherClassSubjectRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId);
        Set<String> keys = new HashSet<>();
        for (TeacherClassSubject t : rows) {
            keys.add(key(t.getClassName(), t.getSubjectName()));
            keys.add(key(t.getClassId(), t.getSubjectName()));
        }
        return keys;
    }

    private static String key(String className, String subjectName) {
        return (className == null ? "" : className.trim().toLowerCase()) + "::"
                + (subjectName == null ? "" : subjectName.trim().toLowerCase());
    }
}
