package com.businessos.auth.role.controller;

import com.businessos.auth.role.dto.PermissionResponse;
import com.businessos.auth.role.entity.Permission;
import com.businessos.auth.role.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Read-only catalog of every permission the app knows about (seeded from PermissionCode at boot). */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PermissionResponse::getCode))
                .toList();
    }

    private PermissionResponse toResponse(Permission p) {
        String module = p.getCode().contains("_") ? p.getCode().substring(0, p.getCode().indexOf('_')) : p.getCode();
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .module(module)
                .build();
    }
}
