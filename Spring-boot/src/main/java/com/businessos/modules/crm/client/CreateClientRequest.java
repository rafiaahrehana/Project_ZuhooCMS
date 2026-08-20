
package com.businessos.modules.crm.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateClientRequest {

    // First/last name, email and password are only required when provisionPortalLogin=true
    // (validated in ClientServiceImpl, not here, since they're conditionally required).
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // When true (default false), a portal login (User) is created and linked to the Client
    // in the same step. When false, the Client is created without a User - a login can be
    // provisioned later. See ClientServiceImpl.create().
    private Boolean provisionPortalLogin = false;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String clientCompanyName;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    private Long accountManagerId;

    private String billingAddress;
    private String shippingAddress;

    @Size(max = 500, message = "Tags must not exceed 500 characters")
    private String tags;

    private Integer employeeCount;
    private BigDecimal annualRevenue;

    private java.util.List<Long> tagIds;
}
