package com.businessos.core.automation;

import lombok.Getter;

/**
 * Published when a ServiceRequest status moves to COMPLETED.
 * Used by UsageBillingService to generate overage invoices.
 */
@Getter
public class ServiceRequestCompletedEvent extends BusinessEvent {

    private final Long serviceRequestId;
    private final Long clientId;

    public ServiceRequestCompletedEvent(Object source, Long companyId,
                                        Long serviceRequestId, Long clientId) {
        super(source, companyId, "SERVICE_REQUEST_COMPLETED");
        this.serviceRequestId = serviceRequestId;
        this.clientId = clientId;
    }
}
