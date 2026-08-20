
package com.businessos.modules.crm.client;

import com.businessos.enums.ClientStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateClientRequest {

    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String clientCompanyName;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    private ClientStatus status;
    private Long accountManagerId;
    private Boolean portalAccessEnabled;

    private String billingAddress;
    private String shippingAddress;

    @Size(max = 500, message = "Tags must not exceed 500 characters")
    private String tags;

    private Integer employeeCount;
    private BigDecimal annualRevenue;

    private java.util.List<Long> tagIds;
}
