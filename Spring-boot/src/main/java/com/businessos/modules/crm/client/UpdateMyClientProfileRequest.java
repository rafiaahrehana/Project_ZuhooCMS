package com.businessos.modules.crm.client;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Fields a CLIENT is allowed to edit on their own profile via PATCH /api/clients/me.
 * Deliberately excludes status, accountManagerId, and portalAccessEnabled - those
 * stay admin/owner-only (see UpdateClientRequest, used by the staff-facing endpoint).
 */
@Data
public class UpdateMyClientProfileRequest {

    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String clientCompanyName;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    private String billingAddress;
    private String shippingAddress;
}
