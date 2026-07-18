package com.srms.api.modules.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.exception.BusinessException;
import com.srms.api.exception.ResourceNotFoundException;
import com.srms.api.modules.assessment.dto.GradingBandDto;
import com.srms.api.modules.school.entity.School;
import com.srms.api.modules.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingScaleService {
    private final SchoolRepository schoolRepository;
    private final ObjectMapper objectMapper;

    public static List<GradingBandDto> zambia2023Defaults() {
        return List.of(
                band(80, 100, "A", "Excellent", 1),
                band(70, 79, "B+", "Very Good", 2),
                band(60, 69, "B", "Good", 3),
                band(50, 59, "C+", "Credit", 4),
                band(40, 49, "C", "Satisfactory", 5),
                band(30, 39, "D+", "Elementary", 6),
                band(20, 29, "D", "Limited Achievement", 7),
                band(0, 19, "E", "Unsatisfactory", 8)
        );
    }

    private static GradingBandDto band(int min, int max, String grade, String description, int points) {
        return GradingBandDto.builder().min(min).max(max).grade(grade)
                .description(description).points(points).build();
    }

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
