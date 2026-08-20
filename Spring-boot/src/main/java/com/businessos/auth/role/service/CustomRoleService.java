package com.businessos.auth.role.service;

import com.businessos.auth.role.dto.CustomRoleRequest;
import com.businessos.auth.role.dto.CustomRoleResponse;

import java.util.List;

public interface CustomRoleService {

    CustomRoleResponse create(CustomRoleRequest request);

    CustomRoleResponse update(Long id, CustomRoleRequest request);

    void delete(Long id);

    List<CustomRoleResponse> getAll();

    CustomRoleResponse getById(Long id);

    /** Permission codes (PermissionCode names) currently assigned to a role. */
    List<String> getPermissions(Long roleId);

    /** Replaces a role's entire permission set with the given codes. */
    List<String> setPermissions(Long roleId, List<String> permissionCodes);

    /** Assigns an employee (by their linked user) to a custom role. */
    void assignEmployee(Long roleId, Long employeeId);

    /** Clears an employee's custom role assignment, if any. */
    void unassignEmployee(Long employeeId);
}
