package com.businessos.modules.servicedesk.billing;
import com.businessos.enums.InvoiceType;
import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.modules.servicedesk.companyservice.PackageSubscriptionRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.modules.finance.invoice.ClientInvoiceRequest;
import com.businessos.modules.finance.invoice.ClientInvoiceItemRequest;
import com.businessos.modules.finance.invoice.ClientInvoiceService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageBillingService {

    private final ServiceRequestRepository   serviceRequestRepository;
    private final PackageSubscriptionRepository subscriptionRepository;
    private final ClientInvoiceService       invoiceService;

    @Transactional
    public void handleCompletion(Long serviceRequestId, Long companyId) {
        ServiceRequest request = serviceRequestRepository.findByIdAndCompanyId(serviceRequestId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));

        PackageSubscription sub = request.getSubscription();
        if (sub == null) {
            return;
        }

        // Do not generate overage invoices for the platform tenant
        if (request.getCompany() != null && request.getCompany().isPlatformTenant()) {
            return;
        }

        sub = subscriptionRepository.findById(sub.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        BigDecimal overageRate = sub.getServicePackage().getOverageRate();
        if (overageRate == null) {
            return;
        }

        Integer quota = sub.getServicePackage().getRequestQuota();
        if (quota == null) {
            return;
        }

        if (sub.getRequestsUsed() <= quota) {
            return;
        }

        ClientInvoiceItemRequest item = ClientInvoiceItemRequest.builder()
            .description("Overage Charge")
            .quantity(new BigDecimal("1"))
            .unitPrice(overageRate)
            .build();

        ClientInvoiceRequest invoice = ClientInvoiceRequest.builder()
            .clientId(request.getClient().getId())
            .invoiceDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(30))
            .notes("Overage charge: service request #" + serviceRequestId
                + " exceeded quota of " + quota + " requests for subscription #" + sub.getId()
                + " (" + sub.getServicePackage().getName() + ")")
            .items(List.of(item))
            .build();

        try {
            var created = invoiceService.create(invoice);
            invoiceService.sendInvoice(created.getId());
        } catch (Exception e) {
            throw new BadRequestException(
                "Failed to generate overage invoice for request " + serviceRequestId + ": " + e.getMessage());
        }
    }
}
