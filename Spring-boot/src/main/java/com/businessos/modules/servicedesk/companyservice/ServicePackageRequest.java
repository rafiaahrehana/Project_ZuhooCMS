package com.businessos.modules.servicedesk.companyservice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.businessos.enums.BillingCycle;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServicePackageRequest {

    @NotBlank(message = "Package name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 150)
    private String nameBn;

    private String description;
    private String descriptionBn;
    private String iconUrl;

    /**
     * Flat price per billing cycle. If null, the system sums the included
     * service prices and applies discountPercent.
     */
    @DecimalMin(value = "0.00", message = "Package price must be zero or positive")
    private BigDecimal packagePrice;

    @DecimalMin(value = "0.0", message = "Discount must be zero or positive")
    private BigDecimal discountPercent;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    /**
     * Max requests per billing period. Leave null for unlimited.
     */
    @Min(value = 1, message = "Request quota must be at least 1")
    private Integer requestQuota;

    private Boolean autoRenew;

    private Integer deliveryDays;
    private String terms;
    private boolean featured;
    private boolean popular;

    /** IDs of CompanyService records to include in this package. */
    private List<Long> serviceIds;
}
