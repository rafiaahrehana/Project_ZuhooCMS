package com.businessos.modules.finance.invoice;

import com.businessos.modules.crm.client.Client;
import com.businessos.auth.user.User;

public class CreditNoteMapper {

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

    public static CreditNoteResponse toResponse(CreditNote entity) {
        if (entity == null) return null;

        ClientInvoice invoice = entity.getClientInvoice();
        User issuedBy = entity.getIssuedBy();
        return CreditNoteResponse.builder()
                .id(entity.getId())
                .creditNoteNumber(entity.getCreditNoteNumber())
                .clientInvoiceId(invoice != null ? invoice.getId() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .clientId(invoice != null && invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(invoice != null ? getClientDisplayName(invoice.getClient()) : null)
                .amount(entity.getAmount())
                .reason(entity.getReason())
                .issuedByName(issuedBy != null ? issuedBy.getFirstName() + " " + issuedBy.getLastName() : null)
                .issuedAt(entity.getIssuedAt())
                .build();
    }
}
