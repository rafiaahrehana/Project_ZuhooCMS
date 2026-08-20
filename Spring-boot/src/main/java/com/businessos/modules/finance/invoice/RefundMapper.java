package com.businessos.modules.finance.invoice;

import com.businessos.modules.crm.client.Client;
import com.businessos.auth.user.User;

public class RefundMapper {

    private static String getClientDisplayName(Client client) {
        if (client == null) return null;
        try {
            if (client.getClientCompanyName() != null && !client.getClientCompanyName().trim().isEmpty()) {
                return client.getClientCompanyName().trim();
            }
        } catch (Exception e) {
            // ignore proxy errors
        }
        try {
            if (client.getUser() != null) {
                String first = client.getUser().getFirstName();
                String last = client.getUser().getLastName();
                if (first != null || last != null) {
                    return ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
                }
            }
        } catch (Exception e) {
            // ignore proxy errors
        }
        return "Client";
    }

    public static RefundResponse toResponse(Refund entity) {
        if (entity == null) return null;

        ClientInvoice invoice = entity.getClientInvoice();
        return RefundResponse.builder()
                .id(entity.getId())
                .clientInvoiceId(invoice != null ? invoice.getId() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .clientId(invoice != null && invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(invoice != null ? getClientDisplayName(invoice.getClient()) : null)
                .serviceRequestId(invoice != null && invoice.getServiceRequest() != null ? invoice.getServiceRequest().getId() : null)
                .serviceRequestTitle(invoice != null && invoice.getServiceRequest() != null ? invoice.getServiceRequest().getTitle() : null)
                .requestedAmount(entity.getRequestedAmount())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .requestedAt(entity.getCreatedAt())
                .processedByName(entity.getProcessedBy() != null ? entity.getProcessedBy().getFirstName() + " " + entity.getProcessedBy().getLastName() : null)
                .processedAt(entity.getProcessedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
}
