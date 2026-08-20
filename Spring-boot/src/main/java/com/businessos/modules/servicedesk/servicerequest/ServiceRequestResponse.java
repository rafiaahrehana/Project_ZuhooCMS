package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.enums.ServiceRequestPriority;
import com.businessos.enums.ServiceRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceRequestResponse {
    private Long id;
    private String title;
    private String description;
    private ServiceRequestStatus status;
    private ServiceRequestPriority priority;
    private BigDecimal agreedPrice;
    private LocalDateTime slaDeadline;
    private boolean slaBreach;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private int resubmitCount;
    private boolean permanentlyClosed;
    private Long companyId;
    private Long clientId;
    private String clientName;
    private Long hubServiceId;
    private String hubServiceName;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private long taskCount;
    private long completedTaskCount;

    // Subscription info — null for standalone (pay-per-request) requests
    private Long subscriptionId;
    private String packageName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String paymentRedirectUrl;
    private Long invoiceId;

    // Filing reference once staff submits this request to a government
    // authority (RJSC, City Corporation, NBR, ...) - e.g. type "Trade License
    // Application" / number "TL-2026-4471".
    private String govRefNumber;
    private String govRefType;

    // Quotation fields
    private BigDecimal quotationAmount;
    private String quotationCurrency;
    private String quotationNotes;
    private LocalDateTime quotationValidUntil;
    private com.businessos.enums.QuotationStatus quotationStatus;

    // Client answers to the service's dynamic form fields, keyed by field id
    private java.util.Map<String, String> formData;

    private String aiSummary;
}
