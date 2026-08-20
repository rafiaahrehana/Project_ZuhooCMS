package com.businessos.auth.role.service;

import com.businessos.auth.role.entity.CustomRole;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.enums.Role;
import com.businessos.auth.role.repository.RolePermissionRepository;
import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final SecurityUtil securityUtil;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public void checkPermission(PermissionCode permission) {

        if (!hasPermission(permission)) {
            throw new ForbiddenException("You do not have permission: " + permission);
        }
    }

    @Override
    public void checkAnyPermission(PermissionCode... permissions) {
        for (PermissionCode permission : permissions) {
            if (hasPermission(permission)) {
                return;
            }
        }
        throw new ForbiddenException("You do not have any of the required permissions: " + Arrays.toString(permissions));
    }

    @Override
    public boolean hasPermission(PermissionCode permission) {

        User user = securityUtil.getCurrentUser();

        // While impersonating a company, act as its owner for fine-grained permission
        // checks too - the real admin's own role (checked below) must not gate tenant
        // modules like Leave/Payroll/AI that use checkPermission() instead of @PreAuthorize.
        if (securityUtil.isImpersonating()) {
            return true;
        }

        // Platform Admins can only access Platform Level permissions (Privacy)
        if (user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.SYSTEM_ADMIN) {
            return isPlatformPermission(permission);
        }

        // Company Owner can do everything inside the company
        if (user.getRole() == Role.COMPANY_OWNER) {
            return true;
        }

        CustomRole role = user.getCustomRole();
        if (role == null) {
            return false;
        }

        return rolePermissionRepository.existsByCustomRoleIdAndPermission_Code(
                role.getId(), permission.name());
    }

    private boolean isPlatformPermission(PermissionCode permission) {
        return switch (permission) {
            case COMPANY_VIEW, COMPANY_UPDATE,
                    USER_VIEW, USER_CREATE, USER_UPDATE, USER_DELETE,
                    AI_ADMIN,
                    TICKET_VIEW, SUPPORT_MESSAGE_VIEW, SUPPORT_CATEGORY_VIEW,
                    SLA_POLICY_VIEW, AUDIT_LOG_VIEW ->
                true;
            default -> false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMyPermissionCodes() {
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            return List.of();
        }

        // Mirrors hasPermission() above: impersonation and COMPANY_OWNER both act as
        // "everything" - enumerate every known code rather than a single wildcard string
        // so the frontend's hasPermission(code) check stays a plain list-membership test
        // with no special-casing needed for the owner.
        if (securityUtil.isImpersonating() || user.getRole() == Role.COMPANY_OWNER) {
            return Arrays.stream(PermissionCode.values()).map(Enum::name).toList();
        }

        if (user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.SYSTEM_ADMIN) {
            return Arrays.stream(PermissionCode.values())
                    .filter(this::isPlatformPermission)
                    .map(Enum::name)
                    .toList();
        }

        CustomRole role = user.getCustomRole();
        if (role == null) {
            return List.of();
        }

        return rolePermissionRepository.findByCustomRoleId(role.getId()).stream()
                .map(rp -> rp.getPermission().getCode())
                .toList();
    }
}