package com.businessos.modules.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCompanyRequest {
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 150)
    private String companyName;
    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$", message = "Subdomain: 3-50 chars, lowercase letters, numbers and hyphens only")
    private String subdomain;
    @NotBlank(message = "Owner first name is required")
    @Size(min = 2, max = 50)
    private String ownerFirstName;
    @NotBlank(message = "Owner last name is required")
    @Size(min = 2, max = 50)
    private String ownerLastName;
    @NotBlank(message = "Owner email is required")
    @Email(message = "Must be a valid email address")
    private String ownerEmail;
    @NotBlank(message = "Owner password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String ownerPassword;
    @Size(max = 30)
    private String companyPhone;
    private com.businessos.shared.address.AddressRequest locationDetail;
}
