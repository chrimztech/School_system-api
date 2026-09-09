package com.srms.api.modules.academic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GradeOffsetFixResult {
    private int classesFixed;
    private int studentsFixed;
    private List<String> details;
}
