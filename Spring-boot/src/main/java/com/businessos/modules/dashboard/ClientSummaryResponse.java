package com.businessos.modules.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @Builder
public class ClientSummaryResponse {

    // Service requests (scoped to the logged-in client)
    long pendingRequests;
    long inProgressRequests;
    long completedRequests;

    // Invoices (scoped to the logged-in client)
    long unpaidInvoices;
    BigDecimal outstandingInvoiceAmount;
}
