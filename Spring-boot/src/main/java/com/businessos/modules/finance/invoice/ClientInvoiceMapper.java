package com.businessos.modules.finance.invoice;

import com.businessos.modules.crm.client.Client;
import com.businessos.auth.user.User;

public class ClientInvoiceMapper {

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

    public static ClientInvoiceResponse toResponse(ClientInvoice entity) {
        if (entity == null) return null;

        return ClientInvoiceResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .invoiceNumber(entity.getInvoiceNumber())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientName(getClientDisplayName(entity.getClient()))
                .serviceRequestId(entity.getServiceRequest() != null ? entity.getServiceRequest().getId() : null)
                .serviceRequestTitle(entity.getServiceRequest() != null ? entity.getServiceRequest().getTitle() : null)
                .invoiceDate(entity.getInvoiceDate())
                .dueDate(entity.getDueDate())
                .items(ClientInvoiceItemMapper.toResponseList(entity.getItems()))
                .subtotal(entity.getSubtotal())
                .taxRatePercent(entity.getTaxRatePercent())
                .taxAmount(entity.getTaxAmount())
                .discountAmount(entity.getDiscountAmount())
                .currency(entity.getCurrency())
                .exchangeRate(entity.getExchangeRate())
                .totalAmount(entity.getTotalAmount())
                .paidAmount(entity.getPaidAmount())
                .creditedAmount(entity.getCreditedAmount())
                .balanceAmount(entity.getBalanceAmount())
                .status(entity.getStatus())
                .paymentTerms(entity.getPaymentTerms())
                .description(entity.getDescription())
                .notes(entity.getNotes())
                .sentDate(entity.getSentDate())
                .paidDate(entity.getPaidDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ClientInvoice toEntity(ClientInvoiceRequest request) {
        if (request == null) return null;

        return ClientInvoice.builder()
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .taxAmount(request.getTaxAmount())
                .paymentTerms(request.getPaymentTerms())
                .description(request.getDescription())
                .notes(request.getNotes())
                .build();
    }
}
