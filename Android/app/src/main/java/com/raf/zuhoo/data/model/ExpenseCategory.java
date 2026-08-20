package com.raf.zuhoo.data.model;

// The backend's `category` field is a freeform string (no server-side enum), so this is a
// client-side curated list for the create-expense spinner, matching the categories the backend
// code comments suggest. Any value still submits fine — nothing rejects an unlisted one.
public final class ExpenseCategory {

    public static final String TRAVEL = "TRAVEL";
    public static final String MEALS = "MEALS";
    public static final String OFFICE_SUPPLIES = "OFFICE_SUPPLIES";
    public static final String TRANSPORT = "TRANSPORT";
    public static final String ACCOMMODATION = "ACCOMMODATION";
    public static final String LICENSING = "LICENSING";
    public static final String OTHER = "OTHER";

    public static final String[] VALUES = {
            TRAVEL, MEALS, OFFICE_SUPPLIES, TRANSPORT, ACCOMMODATION, LICENSING, OTHER
    };

    private ExpenseCategory() {
    }
}
