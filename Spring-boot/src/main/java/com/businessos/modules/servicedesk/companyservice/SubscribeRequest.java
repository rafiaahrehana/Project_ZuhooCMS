package com.businessos.modules.servicedesk.companyservice;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    /**
     * Optional: admin subscribing on behalf of a specific client.
     * If null and caller is CLIENT role, the service resolves clientId
     * from the JWT principal automatically.
     */
    private Long clientId;

    /**
     * Override auto-renew for this specific subscription.
     * Defaults to the package-level autoRenew setting if not provided.
     */
    private Boolean autoRenew;
}
