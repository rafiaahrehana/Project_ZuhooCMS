package com.businessos.modules.hrm.designation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DesignationResponse {
    private Long id;
    private String name;
    private String code;
    private int level;
    private String description;
    private boolean active;
    private String employmentCategory;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
}
