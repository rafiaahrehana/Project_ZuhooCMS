package com.businessos.modules.crm.contact;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ClientContactResponse {
    private Long id;
    private Long clientId;
    private String clientCompanyName;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private String department;
    private boolean primaryContact;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
