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
    void mapsEveryBoundaryToTheCbcCompetencyLevel() {
        assertGrade(100, "1");
        assertGrade(70, "1");
        assertGrade(69.5, "2");
        assertGrade(69, "2");
        assertGrade(60, "2");
        assertGrade(59, "3");
        assertGrade(50, "3");
        assertGrade(49, "4");
        assertGrade(40, "4");
        assertGrade(39, "5");
        assertGrade(0, "5");
    }

    @Test
    void preservesDescriptionsAndPoints() {
        GradingBandDto band = service.evaluate(bands, 75);
        assertEquals("OUTSTANDING", band.getDescription());
        assertEquals(1, band.getPoints());
    }

    private void assertGrade(double percentage, String expected) {
        assertEquals(expected, service.evaluate(bands, percentage).getGrade());
    }
}
