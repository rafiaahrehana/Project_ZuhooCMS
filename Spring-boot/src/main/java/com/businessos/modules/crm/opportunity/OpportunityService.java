package com.businessos.modules.crm.opportunity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OpportunityService {

    OpportunityResponse create(OpportunityRequest request);

    OpportunityResponse createFromLead(Long leadId, OpportunityRequest request);

    Page<OpportunityResponse> listAll(OpportunityStage stage, Long clientId, Long ownerId, Long tagId, String keyword, Pageable pageable);

    OpportunityResponse getById(Long id);

    OpportunityResponse update(Long id, OpportunityRequest request);

    OpportunityResponse changeStage(Long id, ChangeStageRequest request);

    // Preview whether moving this (client-less) Opportunity to WON would auto-link an
    // existing Client. Null means no client-less transition is pending or no match found.
    com.businessos.modules.crm.duplicate.DuplicateMatch previewWonDuplicate(Long id);

    PipelineSummaryResponse getPipelineSummary();

    void delete(Long id);
}
