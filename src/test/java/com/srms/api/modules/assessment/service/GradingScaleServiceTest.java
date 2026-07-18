package com.srms.api.modules.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srms.api.modules.assessment.dto.GradingBandDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradingScaleServiceTest {
    private final GradingScaleService service = new GradingScaleService(null, new ObjectMapper());
    private final List<GradingBandDto> bands = GradingScaleService.zambia2023Defaults();

    @Test
    void mapsEveryBoundaryToTheZambia2023AchievementGrade() {
        assertGrade(100, "A");
        assertGrade(80, "A");
        assertGrade(79.99, "B+");
        assertGrade(79, "B+");
        assertGrade(70, "B+");
        assertGrade(69.5, "B");
        assertGrade(69, "B");
        assertGrade(60, "B");
        assertGrade(59, "C+");
        assertGrade(50, "C+");
        assertGrade(49, "C");
        assertGrade(40, "C");
        assertGrade(39, "D+");
        assertGrade(30, "D+");
        assertGrade(29, "D");
        assertGrade(20, "D");
        assertGrade(19.99, "E");
        assertGrade(19, "E");
        assertGrade(0, "E");
    }

    @Test
    void preservesDescriptionsAndPoints() {
        GradingBandDto band = service.evaluate(bands, 75);
        assertEquals("Very Good", band.getDescription());
        assertEquals(2, band.getPoints());
    }

    private void assertGrade(double percentage, String expected) {
        assertEquals(expected, service.evaluate(bands, percentage).getGrade());
    }
}
