package com.businessos.auth.role.mapper;

import com.businessos.auth.role.dto.CustomRoleRequest;
import com.businessos.auth.role.dto.CustomRoleResponse;
import com.businessos.auth.role.entity.CustomRole;

public class CustomRoleMapper {

    private CustomRoleMapper() {
    }

    public static CustomRole toEntity(CustomRoleRequest request) {

        return CustomRole.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(true)
                .systemRole(false)
                .build();
    }

    public static CustomRoleResponse toResponse(CustomRole role) {

        return CustomRoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.getActive())
                .systemRole(role.getSystemRole())
                .build();
    }

}