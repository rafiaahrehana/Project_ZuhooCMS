package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

// GET /api/dashboard/client-summary. Server-side counts over the client's whole history — unlike
// counting a page of /my, which silently undercounts past the first 20 rows.
//
// Note how the backend groups the statuses (DashboardServiceImpl.getClientSummary):
//   pendingRequests    = PENDING + QUOTATION_PENDING
//   inProgressRequests = IN_PROGRESS + ASSIGNED
// WAITING_CLIENT / UNDER_REVIEW / RESUBMITTED are in neither, so these two don't add up to
// "all open requests" — label the cards for what they actually count.
public class ClientSummaryResponse {

    @SerializedName("pendingRequests")
    private long pendingRequests;
    @SerializedName("inProgressRequests")
    private long inProgressRequests;
    @SerializedName("completedRequests")
    private long completedRequests;
    @SerializedName("unpaidInvoices")
    private long unpaidInvoices;
    @SerializedName("outstandingInvoiceAmount")
    private BigDecimal outstandingInvoiceAmount;

    public long getPendingRequests() {
        return pendingRequests;
    }

    public long getInProgressRequests() {
        return inProgressRequests;
    }

    public long getCompletedRequests() {
        return completedRequests;
    }

    public long getUnpaidInvoices() {
        return unpaidInvoices;
    }

    public BigDecimal getOutstandingInvoiceAmount() {
        return outstandingInvoiceAmount;
    }
}
