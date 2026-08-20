package com.businessos.auth.role.service;

import com.businessos.auth.role.enums.PermissionCode;

import java.util.List;

public interface AuthorizationService {

    void checkPermission(PermissionCode permission);

    /** Passes if the caller holds ANY one of the given permissions - for endpoints shared by two features. */
    void checkAnyPermission(PermissionCode... permissions);

    boolean hasPermission(PermissionCode permission);

    /**
     * The full, resolved set of permission codes the current user holds - same
     * resolution rules as hasPermission(), just enumerated instead of checked one at a
     * time. Used to drive frontend permission-based UI (dashboard widgets, sidebar menu).
     */
    List<String> getMyPermissionCodes();
}