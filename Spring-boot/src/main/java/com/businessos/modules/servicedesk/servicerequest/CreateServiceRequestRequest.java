package com.businessos.modules.servicedesk.servicerequest;

import com.businessos.enums.ServiceRequestPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateServiceRequestRequest {

    @NotBlank(message = "Request title is required")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "Service ID is required")
    private Long hubServiceId;

    private ServiceRequestPriority priority;

    @DecimalMin(value = "0.00")
    private BigDecimal agreedPrice;

    private LocalDateTime slaDeadline;

    /**
     * Optional — if the client is raising this request under an existing
     * active subscription, provide the subscription ID here.
     * The service will validate quota and decrement requestsUsed.
     * When provided, agreedPrice is set to ZERO (included in package).
     */
    private Long subscriptionId;

    /**
     * Payment choice and method — reserved for future payment gateway integration.
     * Currently not used in the create flow; the backend generates an invoice
     * automatically when agreedPrice > 0.
     */
    private com.businessos.enums.PaymentChoice paymentChoice;

    private com.businessos.enums.PaymentMethod paymentMethod;

    /**
     * Answers to the service's dynamic form fields (defined by the admin
     * per service), keyed by ServiceFormField id. Required fields are
     * validated server-side against the service's field definitions.
     */
    private java.util.Map<String, String> formData;
}
