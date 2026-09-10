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
        assertGrade(100, "1");
        assertGrade(75, "1");
        assertGrade(74.99, "2");
        assertGrade(74, "2");
        assertGrade(70, "2");
        assertGrade(69.5, "3");
        assertGrade(69, "3");
        assertGrade(65, "3");
        assertGrade(64, "4");
        assertGrade(60, "4");
        assertGrade(59, "5");
        assertGrade(55, "5");
        assertGrade(54, "6");
        assertGrade(50, "6");
        assertGrade(49, "7");
        assertGrade(45, "7");
        assertGrade(44, "8");
        assertGrade(40, "8");
        assertGrade(39, "9");
        assertGrade(0, "9");
    }

    @Test
    void preservesDescriptionsAndPoints() {
        GradingBandDto band = service.evaluate(bands, 75);
        assertEquals("UPPER DISTINCTION", band.getDescription());
        assertEquals(1, band.getPoints());
    }

    private void assertGrade(double percentage, String expected) {
        assertEquals(expected, service.evaluate(bands, percentage).getGrade());
    }
}
