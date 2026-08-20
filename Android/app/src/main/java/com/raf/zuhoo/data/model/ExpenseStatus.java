package com.raf.zuhoo.data.model;

// Mirrors com.zuhoo.enums.ExpenseStatus on the backend.
public final class ExpenseStatus {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";

    private ExpenseStatus() {
    }
}
