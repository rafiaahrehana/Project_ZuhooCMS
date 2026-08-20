package com.raf.zuhoo.data.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Mirrors backend com.zuhoo.enums.TicketStatus — full value set now confirmed against the enum
// rather than inferred. RESOLVED and CLOSED are the terminal ones; everything else counts as open.
public final class TicketStatus {

    public static final String NEW = "NEW";
    public static final String OPEN = "OPEN";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String WAITING = "WAITING";
    public static final String ON_HOLD = "ON_HOLD";
    public static final String RESOLVED = "RESOLVED";
    public static final String CLOSED = "CLOSED";
    public static final String REOPENED = "REOPENED";

    private static final Set<String> TERMINAL = new HashSet<>(Arrays.asList(RESOLVED, CLOSED));

    private TicketStatus() {
    }

    public static boolean isOpen(String status) {
        return status != null && !TERMINAL.contains(status);
    }
}
