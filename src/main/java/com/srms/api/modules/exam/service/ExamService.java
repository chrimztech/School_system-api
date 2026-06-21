package com.srms.api.modules.exam.service;

import com.srms.api.modules.exam.entity.ExamPaper;
import com.srms.api.modules.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class ExamService {
    private final ExamRepository repo;
    public List<ExamPaper> list(String schoolId) { return repo.findBySchoolIdOrderByExamDateAsc(schoolId); }
    public ExamPaper create(String schoolId, ExamPaper paper) { paper.setSchoolId(schoolId); return repo.save(paper); }
    public ExamPaper update(String schoolId, String id, ExamPaper updated) {
        ExamPaper p = repo.findById(id).filter(e -> e.getSchoolId().equals(schoolId)).orElseThrow();
        p.setSubject(updated.getSubject()); p.setGrade(updated.getGrade()); p.setExamDate(updated.getExamDate());
        p.setStartTime(updated.getStartTime()); p.setDuration(updated.getDuration()); p.setRoom(updated.getRoom());
        p.setCandidates(updated.getCandidates()); p.setInvigilator(updated.getInvigilator()); p.setStatus(updated.getStatus());
        return repo.save(p);
    }
    public void delete(String schoolId, String id) { repo.findById(id).filter(e -> e.getSchoolId().equals(schoolId)).ifPresent(repo::delete); }
}
