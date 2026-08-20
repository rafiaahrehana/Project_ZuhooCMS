package com.raf.zuhoo.data.model;

// Mirrors com.zuhoo.enums.LeadStatus on the backend.
public final class LeadStatus {

    public static final String NEW = "NEW";
    public static final String CONTACTED = "CONTACTED";
    public static final String QUALIFIED = "QUALIFIED";
    public static final String DISQUALIFIED = "DISQUALIFIED";

    public static final String[] VALUES = { NEW, CONTACTED, QUALIFIED, DISQUALIFIED };

    private LeadStatus() {
    }
}
