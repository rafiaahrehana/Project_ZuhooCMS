package com.businessos.modules.finance.payment;

public class PaymentReceiptMapper {
    public static PaymentReceiptResponse toResponse(PaymentReceipt entity) {
        if (entity == null) {
            return null;
        }

        return PaymentReceiptResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .receiptNumber(entity.getReceiptNumber())
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .invoiceNumber(entity.getInvoice() != null ? entity.getInvoice().getInvoiceNumber() : null)
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientName(entity.getClient() != null && entity.getClient().getUser() != null ? entity.getClient().getUser().getFullName() : null)
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .paymentMethod(entity.getPaymentMethod())
                .transactionReference(entity.getTransactionReference())
                .status(entity.getStatus())
                .depositedToBank(entity.getDepositedToBank())
                .reversedDate(entity.getReversedDate())
                .reversalReason(entity.getReversalReason())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
