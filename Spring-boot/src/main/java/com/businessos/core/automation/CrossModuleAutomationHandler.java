package com.businessos.core.automation;

import com.businessos.modules.servicedesk.billing.UsageBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CrossModuleAutomationHandler {

    private final UsageBillingService usageBillingService;

    //When a service request completes, check whether an overage invoice
  //should be generated (usage-based billing).

    @Async
    @EventListener
    public void onServiceRequestCompleted(ServiceRequestCompletedEvent event) {
        try {
            usageBillingService.handleCompletion(event.getServiceRequestId(), event.getCompanyId());
        } catch (Exception ex) {
            log.error("UsageBilling failed for request {}: {}",
                event.getServiceRequestId(), ex.getMessage(), ex);
        }
    }

   //When an opportunity closes as WON, log it for now.
     //Future: auto-create a welcome service request or trigger an email campaign.

    @Async
    @EventListener
    public void onOpportunityWon(OpportunityWonEvent event) {
        try {
            log.info("[Automation] Opportunity WON: id={} client={} company={} name='{}'",
                event.getOpportunityId(), event.getClientId(),
                event.getCompanyId(), event.getOpportunityName());
            // Extension point: add ServiceRequestService.createIntake(event) here
            // when a Welcome Intake workflow template is implemented.
        } catch (Exception ex) {
            log.error("[Automation] Opportunity WON handler failed for opportunity {}: {}",
                event.getOpportunityId(), ex.getMessage(), ex);
        }
    }
}
