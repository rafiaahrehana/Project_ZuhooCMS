package com.businessos.modules.finance.payment;

public enum PaymentStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    DEPOSITED("Deposited to bank"),
    FAILED("Failed"),
    REVERSED("Reversed");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
