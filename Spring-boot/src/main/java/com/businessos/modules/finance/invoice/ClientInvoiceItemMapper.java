package com.businessos.modules.finance.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ClientInvoiceItemMapper {
    public static ClientInvoiceItem toEntity(ClientInvoiceItemRequest request) {
        if (request == null) return null;

        ClientInvoiceItem item = ClientInvoiceItem.builder()
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .notes(request.getNotes())
                .build();
        item.calculateLineTotal();
        return item;
    }

    public static ClientInvoiceItemResponse toResponse(ClientInvoiceItem item) {
        if (item == null) return null;
        return ClientInvoiceItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .notes(item.getNotes())
                .build();
    }

    public static List<ClientInvoiceItemResponse> toResponseList(List<ClientInvoiceItem> items) {
        if (items == null) return List.of();
        return items.stream().map(ClientInvoiceItemMapper::toResponse).collect(Collectors.toList());
    }
}
