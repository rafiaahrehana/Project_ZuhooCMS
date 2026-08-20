package com.businessos.modules.finance.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientInvoiceRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotEmpty(message = "Invoice must have at least one item")
    private List<ClientInvoiceItemRequest> items;

    @DecimalMin(value = "0.0")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    // If provided, overrides taxAmount - the server recomputes tax from
    // (subtotal - discountAmount) * rate / 100.
    @DecimalMin(value = "0.0")
    private BigDecimal taxRatePercent;

    @DecimalMin(value = "0.0")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String currency;

    // Required (>0) when currency differs from the company's base currency;
    // ignored/forced to 1 for base-currency invoices.
    @DecimalMin(value = "0.000001", message = "Exchange rate must be positive")
    private BigDecimal exchangeRate;

    private PaymentTerms paymentTerms;
    private String description;
    private String notes;

    // Set internally when an invoice is auto-generated for a paid service request
    // (see ClientInvoiceServiceImpl#createForServiceRequest) - not user-supplied.
    private Long serviceRequestId;
}