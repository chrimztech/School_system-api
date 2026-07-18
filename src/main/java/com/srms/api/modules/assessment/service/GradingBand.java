package com.srms.api.modules.assessment.service;

/** One row of a school's configurable achievement scale (e.g. 80-100 = A, 1 point). */
public record GradingBand(int min, int max, String grade, String description, int points) {
}
