package com.businessos.auth.role.controller;

import com.businessos.auth.role.dto.CustomRoleRequest;
import com.businessos.auth.role.dto.CustomRoleResponse;
import com.businessos.auth.role.service.CustomRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom-roles")
@RequiredArgsConstructor
public class CustomRoleController {

    private final CustomRoleService customRoleService;

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomRoleResponse create(@Valid @RequestBody CustomRoleRequest request) {
        return customRoleService.create(request);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping
    public List<CustomRoleResponse> getAll() {
        return customRoleService.getAll();
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/{id}")
    public CustomRoleResponse getById(@PathVariable Long id) {
        return customRoleService.getById(id);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PutMapping("/{id}")
    public CustomRoleResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CustomRoleRequest request) {

        return customRoleService.update(id, request);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customRoleService.delete(id);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping("/{id}/permissions")
    public List<String> getPermissions(@PathVariable Long id) {
        return customRoleService.getPermissions(id);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER') or hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PutMapping("/{id}/permissions")
    public List<String> setPermissions(@PathVariable Long id, @RequestBody List<String> permissionCodes) {
        return customRoleService.setPermissions(id, permissionCodes);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @PostMapping("/{id}/employees/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignEmployee(@PathVariable Long id, @PathVariable Long employeeId) {
        customRoleService.assignEmployee(id, employeeId);
    }

    @PreAuthorize("hasRole('COMPANY_OWNER')")
    @DeleteMapping("/employees/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignEmployee(@PathVariable Long employeeId) {
        customRoleService.unassignEmployee(employeeId);
    }
}