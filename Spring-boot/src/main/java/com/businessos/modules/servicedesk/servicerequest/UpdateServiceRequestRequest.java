package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.enums.ServiceRequestPriority;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateServiceRequestRequest {

    @Size(max = 255)
    private String title;

    private String description;
    private ServiceRequestPriority priority;
    private BigDecimal agreedPrice;
    private LocalDateTime slaDeadline;
    private Long assignedEmployeeId;

    // Filing reference once submitted to a government authority.
    @Size(max = 255)
    private String govRefNumber;
    @Size(max = 255)
    private String govRefType;
}
