package com.businessos.modules.hrm.department;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DepartmentResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private boolean active;
    private BigDecimal budget;
    private Long parentDepartmentId;
    private String parentDepartmentName;
    private Long headEmployeeId;
    private String headEmployeeName;
    private long employeeCount;
    private LocalDateTime createdAt;
}
