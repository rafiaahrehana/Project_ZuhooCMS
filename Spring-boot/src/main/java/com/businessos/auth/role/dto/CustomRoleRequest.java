package com.businessos.auth.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomRoleRequest {

    @NotBlank(message = "Role name is required.")
    @Size(max = 100, message = "Role name cannot exceed 100 characters.")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters.")
    private String description;
}