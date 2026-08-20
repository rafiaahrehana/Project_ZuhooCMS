package com.businessos.modules.crm.lead;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeadFilterRequest {
    
    private String keyword;
    
    private LeadStatus status;
    
    private LeadSource source;
    
    private Priority priority;
    
    private Long assignedToId;

    private Long tagId;
    
    private LocalDate expectedCloseDateFrom;
    
    private LocalDate expectedCloseDateTo;
    
    private Boolean hasActivity;
    
    private Boolean isConverted;
    
    private Boolean isUnassigned;
    
    private Boolean isHighPriority;
    
    private String sortBy = "createdAt"; // createdAt, lastActivityAt, expectedCloseDate, priority
    
    private String sortDirection = "DESC"; // ASC, DESC
}
