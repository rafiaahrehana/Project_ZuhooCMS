package com.businessos.modules.hrm.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentRequest {
    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100)
    private String name;
    @Size(max = 20)
    private String code;
    private String description;
    private Long headEmployeeId;
    private Long parentDepartmentId;
    private BigDecimal budget;
}
