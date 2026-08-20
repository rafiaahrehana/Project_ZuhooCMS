package com.raf.zuhoo.data.model;

// Mirrors RegisterRequest's @Pattern on the backend exactly. Client registration's backend DTO
// only requires 8+ chars, but the plan doc calls for enforcing this stricter rule in both forms
// for consistency (docs/android-client-app-plan.md §6).
public final class PasswordPolicy {

    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).{8,}$";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(PATTERN);
    }
}
