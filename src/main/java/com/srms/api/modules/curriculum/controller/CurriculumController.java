package com.srms.api.modules.curriculum.controller;

import com.srms.api.common.ApiResponse;
import com.srms.api.exception.ForbiddenException;
import com.srms.api.modules.curriculum.dto.TopicsSaveRequest;
import com.srms.api.modules.curriculum.entity.CurriculumDocument;
import com.srms.api.modules.curriculum.entity.CurriculumTopic;
import com.srms.api.modules.curriculum.service.CurriculumService;
import com.srms.api.security.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/** Subject syllabus files and the topics taken from them, per subject, grade and term. */
@RestController
@RequestMapping("/api/schools/{schoolId}/curriculum")
@RequiredArgsConstructor
public class CurriculumController {
    static final Set<String> CURRICULUM_MANAGERS = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "HOD");
    static final Set<String> STAFF = Set.of("SUPER_ADMIN", "SCHOOL_ADMIN", "PRINCIPAL", "DEPUTY_HEAD", "HOD", "TEACHER");

    private final CurriculumService service;

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<CurriculumDocument>>> documents(@PathVariable String schoolId, Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.listDocuments(schoolId)));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CurriculumDocument>> upload(
            @PathVariable String schoolId,
            @RequestParam String subjectName,
            @RequestParam Integer grade,
            @RequestParam String academicYear,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        requireRole(auth, CURRICULUM_MANAGERS);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                service.uploadDocument(schoolId, subjectName, grade, academicYear, file, auth.getName())));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, STAFF);
        CurriculumDocument doc = service.findDocument(schoolId, id);
        Resource file = service.downloadDocument(schoolId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName().replace("\"", "") + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId, @PathVariable String id, Authentication auth) {
        requireRole(auth, CURRICULUM_MANAGERS);
        service.deleteDocument(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok("Syllabus file removed", null));
    }

    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<CurriculumTopic>>> topics(
            @PathVariable String schoolId,
            @RequestParam String subjectName,
            @RequestParam Integer grade,
            @RequestParam String term,
            Authentication auth) {
        requireRole(auth, STAFF);
        return ResponseEntity.ok(ApiResponse.ok(service.listTopics(schoolId, subjectName, grade, term)));
    }

    @PutMapping("/topics")
    public ResponseEntity<ApiResponse<List<CurriculumTopic>>> saveTopics(
            @PathVariable String schoolId, @RequestBody TopicsSaveRequest request, Authentication auth) {
        requireRole(auth, CURRICULUM_MANAGERS);
        return ResponseEntity.ok(ApiResponse.ok(service.replaceTopics(schoolId, request)));
    }

    static void requireRole(Authentication auth, Set<String> allowed) {
        if (!allowed.contains(RoleGuard.roleOf(auth))) {
            throw new ForbiddenException("Your role does not have access to curriculum planning");
        }
    }
}
