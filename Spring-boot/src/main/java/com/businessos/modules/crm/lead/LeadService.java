package com.businessos.modules.crm.lead;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {

    // ==================== CRUD ====================
    LeadResponse createLead(LeadRequest request);

    LeadResponse getLeadById(Long id);

    Page<LeadResponse> listLeads(LeadStatus status, Pageable pageable);

    Page<LeadResponse> listMyLeads(Pageable pageable);

    LeadResponse updateLead(Long id, LeadRequest request);

    void deleteLead(Long id);

    // ==================== Search & Filter ====================
    Page<LeadResponse> searchLeads(String keyword, Pageable pageable);

    Page<LeadResponse> filterLeads(LeadFilterRequest filter, Pageable pageable);

    Page<LeadResponse> findLeadsBySource(LeadSource source, Pageable pageable);

    Page<LeadResponse> findLeadsByPriority(Priority priority, Pageable pageable);

    Page<LeadResponse> findUnassignedLeads(Long companyId, Pageable pageable);

    Page<LeadResponse> findHighPriorityOpenLeads(Long companyId, Pageable pageable);

    Page<LeadResponse> findNeverContactedLeads(Pageable pageable);

    Page<LeadResponse> findStalLeads(Pageable pageable);

    // ==================== Conversion ====================
    // Converts a Qualified Lead into an Opportunity. No Client is created here -
    // that happens when the Opportunity reaches Won (see OpportunityService.changeStage).
    com.businessos.modules.crm.opportunity.OpportunityResponse convertToOpportunity(Long id, ConvertToOpportunityRequest request);

    // ==================== Activity Timeline ====================
    com.businessos.modules.crm.activity.CrmActivityResponse addActivity(Long leadId, com.businessos.modules.crm.activity.CrmActivityRequest request);

    Page<com.businessos.modules.crm.activity.CrmActivityResponse> getActivities(Long leadId, Pageable pageable);

    void deleteActivity(Long leadId, Long activityId);

    // ==================== Dashboard & Reporting ====================
    long countLeadsByStatus(LeadStatus status);

    long countActiveLeads();

    long countMyActiveLeads();

    LeadResponse summariseLead(Long id);
}
