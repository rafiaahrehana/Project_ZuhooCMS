package com.businessos.modules.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 150)
    private String companyName;
    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$", message = "Subdomain must be 3-50 characters, lowercase letters, numbers and hyphens only")
    private String subdomain;
    @Email(message = "Must be a valid email address")
    private String companyEmail;
    @Size(max = 30)
    private String companyPhone;
    private com.businessos.shared.address.Address location;
}
