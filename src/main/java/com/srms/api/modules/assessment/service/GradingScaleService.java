package com.srms.api.modules.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.dto.GradingBandDto;
import com.srms.api.config.CacheConfig;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingScaleService {
    private final SchoolRepository schoolRepository;
    private final ObjectMapper objectMapper;

    // Matches the Examinations Council of Zambia's published School Certificate / GCE
    // O-Level scale (see e.g. ECZ's 2025 School Certificate performance report) — each
    // point-pair carries the official Upper/Lower distinction rather than a repeated bare
    // descriptor, so a "2" doesn't just say the same "DISTINCTION" a "1" does.
    public static List<GradingBandDto> zambia2023Defaults() {
        return List.of(
                band(75, 100, "1", "UPPER DISTINCTION", 1),
                band(70, 74, "2", "LOWER DISTINCTION", 2),
                band(65, 69, "3", "UPPER MERIT", 3),
                band(60, 64, "4", "LOWER MERIT", 4),
                band(55, 59, "5", "UPPER CREDIT", 5),
                band(50, 54, "6", "LOWER CREDIT", 6),
                band(45, 49, "7", "UPPER SATISFACTORY", 7),
                band(40, 44, "8", "LOWER SATISFACTORY", 8),
                band(0, 39, "9", "UNSATISFACTORY", 9)
        );
    }

    // Pre-2023-curriculum descriptors — no Upper/Lower split within a point-pair. This is
    // what zambia2023Defaults() itself read as before ECZ's Upper/Lower wording was applied
    // there for Form 1-4; a transitional-cohort legacy Grade 7-12 student finishes under
    // this same scale they started with, not the newer Form wording.
    public static List<GradingBandDto> zambiaLegacyDefaults() {
        return List.of(
                band(75, 100, "1", "DISTINCTION", 1),
                band(70, 74, "2", "DISTINCTION", 2),
                band(65, 69, "3", "MERIT", 3),
                band(60, 64, "4", "MERIT", 4),
                band(55, 59, "5", "CREDIT", 5),
                band(50, 54, "6", "CREDIT", 6),
                band(45, 49, "7", "SATISFACTORY", 7),
                band(40, 44, "8", "SATISFACTORY", 8),
                band(0, 39, "9", "UNSATISFACTORY", 9)
        );
    }

    private static GradingBandDto band(int min, int max, String grade, String description, int points) {
        return GradingBandDto.builder().min(min).max(max).grade(grade)
                .description(description).points(points).build();
    }

    @Cacheable(value = CacheConfig.GRADING_BANDS, key = "#schoolId")
    public List<GradingBandDto> getBands(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", schoolId));
        return readBands(school.getGradingBandsJson());
    }

    public List<GradingBandDto> readBands(String raw) {
        if (raw == null || raw.isBlank()) return zambia2023Defaults();
        try {
            List<GradingBandDto> bands = objectMapper.readValue(raw, new TypeReference<List<GradingBandDto>>() {});
            return validateAndSort(bands);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            return zambia2023Defaults();
        }
    }

    @Cacheable(value = CacheConfig.GRADING_BANDS, key = "'legacy:' + #schoolId")
    public List<GradingBandDto> getLegacyBands(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School", schoolId));
        return readLegacyBands(school.getLegacyGradingBandsJson());
    }

    public List<GradingBandDto> readLegacyBands(String raw) {
        if (raw == null || raw.isBlank()) return zambiaLegacyDefaults();
        try {
            List<GradingBandDto> bands = objectMapper.readValue(raw, new TypeReference<List<GradingBandDto>>() {});
            return validateAndSort(bands);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            return zambiaLegacyDefaults();
        }
    }

    /** The single place that decides which of a school's two scales applies to a grade —
     * everything that grades a result should go through this rather than calling getBands
     * directly, so "legacy Grade 7-12 uses the pre-2023 scale" stays true everywhere at once. */
    public List<GradingBandDto> getBandsForPhase(String schoolId, String classPhase) {
        return "secondary_legacy".equals(classPhase) ? getLegacyBands(schoolId) : getBands(schoolId);
    }

    public String writeBands(List<GradingBandDto> bands) {
        try {
            return objectMapper.writeValueAsString(validateAndSort(bands));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Unable to save the grading scale");
        }
    }

    public GradingBandDto evaluate(String schoolId, double percentage) {
        return evaluate(getBands(schoolId), percentage);
    }

    public GradingBandDto findByGrade(List<GradingBandDto> bands, String grade) {
        if (grade == null) return null;
        return bands.stream().filter(band -> grade.equalsIgnoreCase(band.getGrade())).findFirst().orElse(null);
    }

    public GradingBandDto evaluate(List<GradingBandDto> bands, double percentage) {
        double bounded = Math.max(0, Math.min(100, percentage));
        return bands.stream()
                .filter(band -> bounded >= band.getMin() && bounded < band.getMax() + 1)
                .findFirst()
                .orElseThrow(() -> new BusinessException("The grading scale does not cover " + Math.round(bounded) + "%"));
    }

    private List<GradingBandDto> validateAndSort(List<GradingBandDto> input) {
        if (input == null || input.isEmpty()) {
            throw new BusinessException("At least one grading band is required");
        }
        List<GradingBandDto> bands = new ArrayList<>(input);
        bands.sort(Comparator.comparingInt(GradingBandDto::getMin));
        int expectedMin = 0;
        for (GradingBandDto band : bands) {
            if (band.getMin() != expectedMin || band.getMax() < band.getMin() || band.getMax() > 100) {
                throw new BusinessException("Grading bands must cover 0-100 without gaps or overlaps");
            }
            if (band.getGrade() == null || band.getGrade().isBlank()
                    || band.getDescription() == null || band.getDescription().isBlank()) {
                throw new BusinessException("Every grading band requires a grade and description");
            }
            expectedMin = band.getMax() + 1;
        }
        if (expectedMin != 101) {
            throw new BusinessException("Grading bands must cover 0-100 without gaps or overlaps");
        }
        bands.sort(Comparator.comparingInt(GradingBandDto::getMin).reversed());
        return bands;
    }
}
