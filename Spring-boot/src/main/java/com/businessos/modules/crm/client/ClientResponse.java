
package com.businessos.modules.crm.client;

import com.businessos.enums.ClientStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ClientResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String image;
    private String clientCompanyName;
    private String industry;
    private String website;
    private String taxId;
    private ClientStatus status;
    private boolean portalAccessEnabled;
    private Long accountManagerId;
    private String accountManagerName;
    private LocalDate onboardedAt;
    private LocalDateTime createdAt;

    // Account-level fields
    private String billingAddress;
    private String shippingAddress;
    private String tags;
    private Integer employeeCount;
    private BigDecimal annualRevenue;
    private BigDecimal lifetimeValue;
    private Integer totalRequests;

    // Normalized shared-taxonomy tags (distinct from the legacy free-text `tags` field above).
    private java.util.List<com.businessos.modules.crm.tag.TagResponse> tagList;

    // Set only right after creation, when a possible-duplicate Client was found.
    // A nudge, not a block - the Client is created either way.
    private com.businessos.modules.crm.duplicate.DuplicateMatch possibleDuplicate;

    /**
     * Only populated by inviteToPortal(): whether the invite email actually left
     * the server. Null on every other response.
     *
     * The login is created either way, so a false here means "account ready, but
     * the client has not been told" - which needs a different message to staff
     * than plain success.
     */
    private Boolean inviteEmailSent;
    /** Why the invite email failed, when inviteEmailSent is false. */
    private String inviteEmailError;
}
