package com.businessos.modules.crm.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClientContactRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 100)
    private String jobTitle;

    @Size(max = 100)
    private String department;

    private Boolean primaryContact;

    private String notes;
}
