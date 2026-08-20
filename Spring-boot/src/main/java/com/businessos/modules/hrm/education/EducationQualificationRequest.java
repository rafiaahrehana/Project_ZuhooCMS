package com.businessos.modules.hrm.education;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EducationQualificationRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    @NotBlank(message = "Degree/qualification level is required")
    private String degree;
    @NotBlank(message = "Institution is required")
    private String institution;
    private String fieldOfStudy;
    private Integer passingYear;
    private String result;
    private String notes;
}
