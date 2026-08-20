package com.businessos.modules.crm.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CrmActivityService {

    CrmActivityResponse log(CrmActivityRequest request);

    //Used by other CRM services (e.g. opportunities) to record
    //system-generated timeline entries such as stage changes.

    void logSystemActivity(CrmActivityType type, String subject, String description,
                           Long clientId, Long opportunityId);

    Page<CrmActivityResponse> getTimeline(Long clientId, Long opportunityId, Pageable pageable);

    CrmActivityResponse markCompleted(Long id);

    void delete(Long id);

    /** Summarise recent activity for a client or opportunity with AI and suggest a next action */
    CrmActivitySummaryResponse summarise(Long clientId, Long opportunityId);
}
