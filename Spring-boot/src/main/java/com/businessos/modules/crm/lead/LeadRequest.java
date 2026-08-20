package com.businessos.modules.crm.lead;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeadRequest {
    
    @NotBlank(message = "Contact name is required")
    @Size(min = 2, max = 150, message = "Contact name must be between 2 and 150 characters")
    private String contactName;
    
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String companyName;
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$|^$", message = "Phone must be a valid phone number")
    private String phone;
    
    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;
    
    @Size(max = 100, message = "Job title must not exceed 100 characters")
    private String jobTitle;
    
    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    private LeadStatus status;
    
    private LeadSource source;

    @Size(max = 150, message = "Source detail must not exceed 150 characters")
    private String sourceOther;

    private Priority priority;
    
    @DecimalMin(value = "0.00", message = "Estimated value must be 0 or greater")
    @DecimalMax(value = "999999999.99", message = "Estimated value is too large")
    private BigDecimal estimatedValue;
    
    @FutureOrPresent(message = "Expected close date must be today or in the future")
    private LocalDate expectedCloseDate;
    
    private Long assignedToId;
    
    private Long interestedServiceId;

    private java.util.List<Long> tagIds;
}
