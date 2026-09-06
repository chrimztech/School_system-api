package com.srms.api.modules.assessment.service;
import com.srms.api.config.CacheConfig;
import com.srms.api.exception.BusinessException;
import com.srms.api.modules.assessment.entity.GradeWeightConfig;
import com.srms.api.modules.assessment.repository.GradeWeightConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class GradeWeightConfigService {
    private final GradeWeightConfigRepository repository;

    // Read on every score save and every term-grade (re)computation — see TermGradeService —
    // to combine CA/midterm/exam into a weighted total, yet only ever written from Settings.
    @Cacheable(value = CacheConfig.GRADE_WEIGHTS, key = "#schoolId")
    public GradeWeightConfig get(String schoolId) {
        return repository.findBySchoolId(schoolId)
                .orElse(GradeWeightConfig.builder().schoolId(schoolId).caWeight(30).midtermWeight(30).examWeight(40).build());
    }

    @CacheEvict(value = CacheConfig.GRADE_WEIGHTS, key = "#schoolId")
    public GradeWeightConfig upsert(String schoolId, int caWeight, int midtermWeight, int examWeight) {
        if (caWeight < 0 || midtermWeight < 0 || examWeight < 0
                || caWeight + midtermWeight + examWeight != 100) {
            throw new BusinessException("Grade weights must be non-negative and sum to 100");
        }
        GradeWeightConfig config = repository.findBySchoolId(schoolId)
                .orElse(GradeWeightConfig.builder().schoolId(schoolId).build());
        config.setCaWeight(caWeight);
        config.setMidtermWeight(midtermWeight);
        config.setExamWeight(examWeight);
        return repository.save(config);
    }
}
