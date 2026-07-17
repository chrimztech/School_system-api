package com.srms.api.modules.discipline.service;

import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.discipline.dto.DisciplineCaseDto;
import com.srms.api.modules.discipline.entity.DisciplineCase;
import com.srms.api.modules.discipline.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;

    @Transactional(readOnly = true)
    public List<DisciplineCase> findAll(String schoolId) {
        return disciplineRepository.findBySchoolIdOrderByIncidentDateDesc(schoolId);
    }

    @Transactional(readOnly = true)
    public Page<DisciplineCase> findAllPaged(String schoolId, Pageable pageable) {
        return disciplineRepository.findBySchoolIdOrderByIncidentDateDesc(schoolId, pageable);
    }

    @Transactional(readOnly = true)
    public DisciplineCase findById(String schoolId, String id) {
        return disciplineRepository.findById(id)
                .filter(dc -> dc.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("DisciplineCase", id));
    }

    @Transactional(readOnly = true)
    public List<DisciplineCase> findByStudent(String schoolId, String studentId) {
        return disciplineRepository.findBySchoolIdAndStudentId(schoolId, studentId);
    }

    public DisciplineCase create(String schoolId, DisciplineCaseDto dto) {
        DisciplineCase dc = DisciplineCase.builder()
                .schoolId(schoolId)
                .studentId(dto.getStudentId())
                .studentName(dto.getStudentName())
                .grade(dto.getGrade())
                .offense(dto.getOffense())
                .offenseCategory(dto.getOffenseCategory())
                .severity(dto.getSeverity())
                .action(dto.getAction())
                .incidentDate(dto.getIncidentDate() != null ? dto.getIncidentDate() : LocalDate.now())
                .incidentTime(dto.getIncidentTime())
                .location(dto.getLocation())
                .witnessNames(dto.getWitnessNames())
                .followUpDate(dto.getFollowUpDate())
                .parentNotified(dto.isParentNotified())
                .repeatCount(dto.getRepeatCount())
                .reportedBy(dto.getReportedBy())
                .status(dto.getStatus() != null ? dto.getStatus() : "OPEN")
                .notes(dto.getNotes())
                .build();
        return disciplineRepository.save(dc);
    }

    public DisciplineCase update(String schoolId, String id, DisciplineCaseDto dto) {
        DisciplineCase dc = disciplineRepository.findById(id)
                .filter(d -> d.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("DisciplineCase", id));
        dc.setStudentId(dto.getStudentId());
        dc.setStudentName(dto.getStudentName());
        dc.setGrade(dto.getGrade());
        dc.setOffense(dto.getOffense());
        dc.setOffenseCategory(dto.getOffenseCategory());
        dc.setSeverity(dto.getSeverity());
        dc.setAction(dto.getAction());
        if (dto.getIncidentDate() != null) dc.setIncidentDate(dto.getIncidentDate());
        dc.setIncidentTime(dto.getIncidentTime());
        dc.setLocation(dto.getLocation());
        dc.setWitnessNames(dto.getWitnessNames());
        dc.setFollowUpDate(dto.getFollowUpDate());
        dc.setParentNotified(dto.isParentNotified());
        dc.setRepeatCount(dto.getRepeatCount());
        dc.setReportedBy(dto.getReportedBy());
        if (dto.getStatus() != null) dc.setStatus(dto.getStatus());
        dc.setNotes(dto.getNotes());
        return disciplineRepository.save(dc);
    }

    public void delete(String schoolId, String id) {
        DisciplineCase dc = disciplineRepository.findById(id)
                .filter(d -> d.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("DisciplineCase", id));
        disciplineRepository.delete(dc);
    }

    public DisciplineCase resolve(String schoolId, String id) {
        DisciplineCase dc = disciplineRepository.findById(id)
                .filter(d -> d.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new ResourceNotFoundException("DisciplineCase", id));
        dc.setStatus("RESOLVED");
        return disciplineRepository.save(dc);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String schoolId) {
        List<DisciplineCase> all = disciplineRepository.findBySchoolIdOrderByIncidentDateDesc(schoolId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("open", disciplineRepository.countBySchoolIdAndStatus(schoolId, "OPEN"));
        stats.put("resolved", disciplineRepository.countBySchoolIdAndStatus(schoolId, "RESOLVED"));
        stats.put("escalated", disciplineRepository.countBySchoolIdAndStatus(schoolId, "ESCALATED"));
        return stats;
    }
}
