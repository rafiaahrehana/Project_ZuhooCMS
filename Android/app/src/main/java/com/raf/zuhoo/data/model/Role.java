package com.raf.zuhoo.data.model;

// Only these roles get a UI in this app; other backend roles (SUPER_ADMIN, SUPPORT_AGENT, etc.)
// manage the SaaS itself and are out of scope (see docs/android-client-app-plan.md).
public final class Role {

    public static final String CLIENT = "CLIENT";
    public static final String COMPANY_OWNER = "COMPANY_OWNER";
    public static final String EMPLOYEE = "EMPLOYEE";

    private Role() {
    }

    public static boolean isSupported(String role) {
        return CLIENT.equals(role) || COMPANY_OWNER.equals(role) || EMPLOYEE.equals(role);
    }
}
