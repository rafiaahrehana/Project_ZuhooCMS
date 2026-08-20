package com.raf.zuhoo.data.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Mirrors ServiceRequestStatus enum values documented in docs/android-client-app-plan.md §5.2.
public final class ServiceRequestStatus {

    public static final String PENDING = "PENDING";
    public static final String QUOTATION_PENDING = "QUOTATION_PENDING";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String WAITING_CLIENT = "WAITING_CLIENT";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String RESUBMITTED = "RESUBMITTED";

    private static final Set<String> TERMINAL = new HashSet<>(
            Arrays.asList(COMPLETED, REJECTED, CANCELLED));

    private ServiceRequestStatus() {
    }

    public static boolean isOpen(String status) {
        return status != null && !TERMINAL.contains(status);
    }
}
