package com.businessos.core.automation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishOpportunityWon(Object source, Long companyId,
                                       Long opportunityId, Long clientId,
                                       String opportunityName) {
        publisher.publishEvent(
            new OpportunityWonEvent(source, companyId, opportunityId, clientId, opportunityName));
    }

    public void publishServiceRequestCompleted(Object source, Long companyId,
                                                Long serviceRequestId, Long clientId) {
        publisher.publishEvent(
            new ServiceRequestCompletedEvent(source, companyId, serviceRequestId, clientId));
    }
}
