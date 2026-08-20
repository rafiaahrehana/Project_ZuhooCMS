package com.businessos.auth.role.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {
    private Long id;
    private String code;
    private String name;
    private String description;

    /** Derived from the code prefix (e.g. "EMPLOYEE_CREATE" -> "EMPLOYEE") for grouping in the UI. */
    private String module;
}
