package com.srms.api.modules.curriculum.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.curriculum.dto.TopicsSaveRequest;
import com.srms.api.modules.curriculum.entity.CurriculumDocument;
import com.srms.api.modules.curriculum.entity.CurriculumTopic;
import com.srms.api.modules.curriculum.repository.CurriculumDocumentRepository;
import com.srms.api.modules.curriculum.repository.CurriculumTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CurriculumService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Value("${app.curriculum.storage-dir:./curriculum-files}")
    private String storageDir;

    private final CurriculumDocumentRepository documents;
    private final CurriculumTopicRepository topics;

    public List<CurriculumDocument> listDocuments(String schoolId) {
        return documents.findBySchoolIdOrderBySubjectNameAscGradeAscVersionDesc(schoolId);
    }

    public CurriculumDocument uploadDocument(String schoolId, String subjectName, Integer grade, String academicYear,
                                             MultipartFile file, String actor) {
        requireSafeSegment(schoolId);
        if (isBlank(subjectName) || grade == null || isBlank(academicYear)) {
            throw new BusinessException("Subject, grade and academic year are all required");
        }
        if (file == null || file.isEmpty()) throw new BusinessException("Choose a syllabus file to upload");
        if (file.getSize() > MAX_BYTES) throw new BusinessException("Syllabus files must be 10 MB or smaller");
        String original = file.getOriginalFilename() == null ? "syllabus" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Upload a PDF, Word document or text file");
        }

        int version = documents.findFirstBySchoolIdAndSubjectNameAndGradeAndAcademicYearOrderByVersionDesc(
                        schoolId, subjectName.trim(), grade, academicYear.trim())
                .map(d -> d.getVersion() + 1).orElse(1);

        String storedName = UUID.randomUUID() + "." + extension;
        Path schoolDir = baseDir().resolve(schoolId);
        Path target = schoolDir.resolve(storedName);
        try {
            Files.createDirectories(schoolDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("Could not save the syllabus file — try again");
        }

        CurriculumDocument doc = new CurriculumDocument();
        doc.setSchoolId(schoolId);
        doc.setSubjectName(subjectName.trim());
        doc.setGrade(grade);
        doc.setAcademicYear(academicYear.trim());
        doc.setVersion(version);
        doc.setFileName(original);
        doc.setContentType(file.getContentType());
        doc.setStoragePath(schoolId + "/" + storedName);
        doc.setSizeBytes(file.getSize());
        doc.setUploadedBy(actor);
        return documents.save(doc);
    }

    public Resource downloadDocument(String schoolId, String id) {
        CurriculumDocument doc = documents.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum document", id));
        Path base = baseDir();
        Path file = base.resolve(doc.getStoragePath()).normalize();
        if (!file.startsWith(base) || !Files.exists(file)) {
            throw new ResourceNotFoundException("Curriculum document file", id);
        }
        return new FileSystemResource(file);
    }

    public CurriculumDocument findDocument(String schoolId, String id) {
        return documents.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculum document", id));
    }

    public void deleteDocument(String schoolId, String id) {
        CurriculumDocument doc = findDocument(schoolId, id);
        try {
            Path base = baseDir();
            Path file = base.resolve(doc.getStoragePath()).normalize();
            if (file.startsWith(base)) Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BusinessException("Could not remove the stored file — try again");
        }
        documents.delete(doc);
    }

    public List<CurriculumTopic> listTopics(String schoolId, String subjectName, Integer grade, String term) {
        return topics.findBySchoolIdAndSubjectNameAndGradeAndTermOrderBySequenceAsc(schoolId, subjectName, grade, term);
    }

    public List<CurriculumTopic> replaceTopics(String schoolId, TopicsSaveRequest req) {
        if (isBlank(req.subjectName()) || req.grade() == null || isBlank(req.term())) {
            throw new BusinessException("Subject, grade and term are all required");
        }
        topics.deleteBySchoolIdAndSubjectNameAndGradeAndTerm(schoolId, req.subjectName(), req.grade(), req.term());
        List<CurriculumTopic> rows = new java.util.ArrayList<>();
        int sequence = 1;
        for (TopicsSaveRequest.TopicInput input : req.topics() == null ? List.<TopicsSaveRequest.TopicInput>of() : req.topics()) {
            if (isBlank(input.topic())) continue;
            CurriculumTopic t = new CurriculumTopic();
            t.setSchoolId(schoolId);
            t.setSubjectName(req.subjectName());
            t.setGrade(req.grade());
            t.setTerm(req.term());
            t.setSequence(sequence++);
            t.setTopic(input.topic().trim());
            t.setSubTopics(input.subTopics());
            t.setObjectives(input.objectives());
            t.setActivities(input.activities());
            t.setResources(input.resources());
            t.setAssessment(input.assessment());
            rows.add(t);
        }
        return topics.saveAll(rows);
    }

    private Path baseDir() {
        return Paths.get(storageDir).toAbsolutePath().normalize();
    }

    private static void requireSafeSegment(String schoolId) {
        if (schoolId == null || !SAFE_SEGMENT.matcher(schoolId).matches()) {
            throw new BusinessException("Invalid school reference");
        }
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
