package com.businessos.auth.role.service;

import com.businessos.auth.role.dto.CustomRoleRequest;
import com.businessos.auth.role.dto.CustomRoleResponse;
import com.businessos.auth.role.entity.CustomRole;
import com.businessos.auth.role.entity.Permission;
import com.businessos.auth.role.entity.RolePermission;
import com.businessos.auth.role.mapper.CustomRoleMapper;
import com.businessos.auth.role.repository.CustomRoleRepository;
import com.businessos.auth.role.repository.PermissionRepository;
import com.businessos.auth.role.repository.RolePermissionRepository;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.audit.AuditService;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomRoleServiceImpl implements CustomRoleService {

    private final CustomRoleRepository customRoleRepository;
    private final CompanyRepository companyRepository;
    private final com.businessos.auth.user.UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    @Override
    public CustomRoleResponse create(CustomRoleRequest request) {

        Long companyId = securityUtil.getCurrentCompanyId();

        if (customRoleRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.getName())) {
            throw new BadRequestException("Role already exists.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        CustomRole role = CustomRoleMapper.toEntity(request);
        role.setCompany(company);
        role = customRoleRepository.save(role);
        auditService.log(AuditEntityType.ROLE, role.getId(), AuditAction.CREATE,
                null, role.getName(), securityUtil.getCurrentUser(), companyId, null);
        return CustomRoleMapper.toResponse(role);
    }

    @Override
    public CustomRoleResponse update(Long id, CustomRoleRequest request) {

        Long companyId = securityUtil.getCurrentCompanyId();

        CustomRole role = customRoleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new BadRequestException("System roles cannot be updated.");
        }

        // Guard against renaming to an existing role name within the same company
        if (!role.getName().equalsIgnoreCase(request.getName()) &&
            customRoleRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.getName())) {
            throw new BadRequestException("A role with that name already exists.");
        }

        role.setName(request.getName().trim());
        role.setDescription(request.getDescription());

        return CustomRoleMapper.toResponse(role);
    }

    @Override
    public void delete(Long id) {

        Long companyId = securityUtil.getCurrentCompanyId();

        CustomRole role = customRoleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new BadRequestException("System roles cannot be deleted.");
        }

        // Deleting used to silently strip every assigned user's permissions with
        // no warning - clearCustomRoleForAllUsers() below wipes the role from
        // them instantly, and nobody found out until they lost access. Block it
        // instead so an admin has to reassign those users first.
        long assignedUsers = userRepository.countByCustomRoleId(role.getId());
        if (assignedUsers > 0) {
            throw new BadRequestException(
                "Cannot delete this role: it is currently assigned to " + assignedUsers
                    + " user(s). Reassign them to a different role first.");
        }

        // Nullify customRole FK on all users before soft-deleting.
        // Replaces the invalid CascadeType.SET_NULL that was removed from User.customRole.
        userRepository.clearCustomRoleForAllUsers(role.getId());
        rolePermissionRepository.deleteByCustomRoleId(role.getId());

        role.softDelete();
        customRoleRepository.save(role);
        auditService.log(AuditEntityType.ROLE, role.getId(), AuditAction.DELETE,
                role.getName(), null, securityUtil.getCurrentUser(), companyId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomRoleResponse> getAll() {

        Long companyId = securityUtil.getCurrentCompanyId();

        return customRoleRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(CustomRoleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomRoleResponse getById(Long id) {

        Long companyId = securityUtil.getCurrentCompanyId();

        CustomRole role = customRoleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        return CustomRoleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPermissions(Long roleId) {
        CustomRole role = findInTenant(roleId);
        return rolePermissionRepository.findByCustomRoleId(role.getId())
                .stream()
                .map(rp -> rp.getPermission().getCode())
                .toList();
    }

    @Override
    public List<String> setPermissions(Long roleId, List<String> permissionCodes) {
        Long companyId = securityUtil.getCurrentCompanyId();
        CustomRole role = findInTenant(roleId);
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new BadRequestException("System roles cannot be modified.");
        }

        List<String> before = getPermissions(roleId);

        // Flush the delete before re-inserting - Hibernate's default flush order runs
        // inserts before deletes within the same flush, so without this, re-saving an
        // unchanged (or overlapping) permission set violates the
        // (custom_role_id, permission_id) unique constraint on every save.
        rolePermissionRepository.deleteByCustomRoleId(role.getId());
        rolePermissionRepository.flush();

        List<String> codes = permissionCodes == null ? List.of() : permissionCodes;
        for (String code : codes) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown permission: " + code));
            rolePermissionRepository.save(RolePermission.builder()
                    .customRole(role)
                    .permission(permission)
                    .build());
        }

        List<String> after = getPermissions(roleId);
        // The one action here with real teeth: this can grant a role access to
        // salary data, financial records, or anything else in the permission
        // catalog - previously this was the one write path in the whole class
        // that left no audit trail at all.
        auditService.log(AuditEntityType.ROLE, role.getId(), AuditAction.PERMISSION_CHANGE,
                String.join(",", before), String.join(",", after), securityUtil.getCurrentUser(), companyId, null);

        return after;
    }

    @Override
    public void assignEmployee(Long roleId, Long employeeId) {
        Long companyId = securityUtil.getCurrentCompanyId();
        CustomRole role = findInTenant(roleId);

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        User user = employee.getUser();
        if (user == null) {
            throw new BadRequestException("Employee has no linked user account");
        }
        user.setCustomRole(role);
        userRepository.save(user);
    }

    @Override
    public void unassignEmployee(Long employeeId) {
        Long companyId = securityUtil.getCurrentCompanyId();

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        User user = employee.getUser();
        if (user == null) {
            throw new BadRequestException("Employee has no linked user account");
        }
        user.setCustomRole(null);
        userRepository.save(user);
    }

    private CustomRole findInTenant(Long roleId) {
        Long companyId = securityUtil.getCurrentCompanyId();
        return customRoleRepository.findByIdAndCompanyId(roleId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
    }
}
