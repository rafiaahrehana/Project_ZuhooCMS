package com.businessos.modules.hrm.designation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignationRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
    @NotBlank(message = "Code is required")
    @Size(max = 30)
    private String code;
    @NotNull(message = "Level is required")
    @Min(value = 1, message = "Level must be at least 1")
    private Integer level;
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    private Long departmentId;
    private String employmentCategory;
    private Boolean active;
}
