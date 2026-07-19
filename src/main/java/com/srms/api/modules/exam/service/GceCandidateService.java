package com.srms.api.modules.exam.service;

import com.srms.api.common.BulkImportResult;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.exam.dto.GceCandidateDto;
import com.srms.api.modules.exam.entity.GceCandidate;
import com.srms.api.modules.exam.repository.GceCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GceCandidateService {
    private final GceCandidateRepository repo;

    public List<GceCandidate> list(String schoolId) {
        return repo.findBySchoolIdOrderByLastNameAsc(schoolId);
    }

    public GceCandidate findById(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("GceCandidate", id));
    }

    public GceCandidate create(String schoolId, GceCandidateDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().isBlank())
            throw new IllegalArgumentException("First name is required");
        if (dto.getLastName() == null || dto.getLastName().isBlank())
            throw new IllegalArgumentException("Last name is required");
        GceCandidate c = new GceCandidate();
        c.setSchoolId(schoolId);
        mapDto(c, dto);
        if (c.getStatus() == null || c.getStatus().isBlank()) c.setStatus("REGISTERED");
        return repo.save(c);
    }

    public BulkImportResult bulkCreate(String schoolId, List<GceCandidateDto> dtos) {
        int imported = 0;
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            try {
                create(schoolId, dtos.get(i));
                imported++;
            } catch (Exception e) {
                errors.add(new BulkImportResult.RowError(i, e.getMessage()));
            }
        }
        return new BulkImportResult(imported, errors);
    }

    public GceCandidate update(String id, String schoolId, GceCandidateDto dto) {
        GceCandidate c = findById(id, schoolId);
        mapDto(c, dto);
        return repo.save(c);
    }

    public void delete(String id, String schoolId) {
        GceCandidate c = findById(id, schoolId);
        repo.delete(c);
    }

    private void mapDto(GceCandidate c, GceCandidateDto dto) {
        if (dto.getExamNumber() != null) c.setExamNumber(dto.getExamNumber());
        if (dto.getFirstName() != null) c.setFirstName(dto.getFirstName());
        if (dto.getMiddleName() != null) c.setMiddleName(dto.getMiddleName());
        if (dto.getLastName() != null) c.setLastName(dto.getLastName());
        if (dto.getGender() != null) c.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) c.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getNrc() != null) c.setNrc(dto.getNrc());
        if (dto.getGrade() != null) c.setGrade(dto.getGrade());
        if (dto.getSubjects() != null) c.setSubjects(dto.getSubjects());
        if (dto.getCenterNumber() != null) c.setCenterNumber(dto.getCenterNumber());
        if (dto.getPhone() != null) c.setPhone(dto.getPhone());
        if (dto.getEmail() != null) c.setEmail(dto.getEmail());
        if (dto.getAddress() != null) c.setAddress(dto.getAddress());
        if (dto.getPreviousSchool() != null) c.setPreviousSchool(dto.getPreviousSchool());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) c.setStatus(dto.getStatus());
    }
}
