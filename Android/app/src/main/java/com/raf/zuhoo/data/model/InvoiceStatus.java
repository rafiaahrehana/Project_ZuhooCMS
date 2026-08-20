package com.raf.zuhoo.data.model;

// Mirrors backend com.zuhoo.enums.InvoiceStatus. VOIDED and REFUNDED are recent additions that
// came in with the credit-note and refund work.
public final class InvoiceStatus {

    public static final String DRAFT = "DRAFT";
    public static final String ISSUED = "ISSUED";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String PAID = "PAID";
    public static final String OVERDUE = "OVERDUE";
    public static final String CANCELLED = "CANCELLED";
    public static final String VOIDED = "VOIDED";
    public static final String REFUNDED = "REFUNDED";

    private InvoiceStatus() {
    }
}
