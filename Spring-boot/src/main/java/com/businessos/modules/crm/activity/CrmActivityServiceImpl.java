package com.businessos.modules.crm.activity;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.CrmActivitySummaryPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.opportunity.Opportunity;
import com.businessos.modules.crm.opportunity.OpportunityRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CrmActivityServiceImpl implements CrmActivityService {

    private final CrmActivityRepository crmActivityRepository;
    private final ClientRepository clientRepository;
    private final OpportunityRepository opportunityRepository;
    private final SecurityUtil securityUtil;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;

    @Override
    public CrmActivityResponse log(CrmActivityRequest request) {
        Long companyId = requireCompanyId();
        if (request.getClientId() == null && request.getOpportunityId() == null) {
            throw new BadRequestException("An activity must reference a client or an opportunity");
        }

        CrmActivity.CrmActivityBuilder builder = CrmActivity.builder()
            .type(request.getType())
            .subject(request.getSubject())
            .description(request.getDescription())
            .activityDate(request.getActivityDate() != null ? request.getActivityDate() : LocalDateTime.now())
            .scheduledAt(request.getScheduledAt())
            .completed(request.getCompleted() == null || request.getCompleted())
            .systemGenerated(false)
            .performedBy(securityUtil.getCurrentUser());

        Client client = null;
        Opportunity opportunity = null;

        if (request.getOpportunityId() != null) {
            opportunity = opportunityRepository.findByIdAndCompanyId(request.getOpportunityId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
            client = opportunity.getClient();
            opportunity.setLastActivityAt(LocalDateTime.now());
            opportunity.setStaleNotifiedAt(null);
        }
        if (request.getClientId() != null) {
            client = clientRepository.findByIdAndCompanyId(request.getClientId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            if (opportunity != null && opportunity.getClient() != null
                    && !client.getId().equals(opportunity.getClient().getId())) {
                throw new BadRequestException("Client does not belong to the referenced opportunity");
            }
        }

        CrmActivity activity = builder
            .client(client)
            .opportunity(opportunity)
            .company(client != null ? client.getCompany() : opportunity.getCompany())
            .build();

        return CrmActivityMapper.toResponse(crmActivityRepository.save(activity));
    }

    @Override
    public void logSystemActivity(CrmActivityType type, String subject, String description,
                                  Long clientId, Long opportunityId) {
        Long companyId = requireCompanyId();

        Client client = clientId != null
            ? clientRepository.findByIdAndCompanyId(clientId, companyId).orElse(null)
            : null;
        Opportunity opportunity = opportunityId != null
            ? opportunityRepository.findByIdAndCompanyId(opportunityId, companyId).orElse(null)
            : null;

        if (client == null && opportunity == null) {
            return; // nothing to attach the timeline entry to
        }

        CrmActivity activity = CrmActivity.builder()
            .type(type)
            .subject(subject)
            .description(description)
            .activityDate(LocalDateTime.now())
            .completed(true)
            .systemGenerated(true)
            .performedBy(securityUtil.getCurrentUser())
            .client(client)
            .opportunity(opportunity)
            .company(client != null ? client.getCompany() : opportunity.getCompany())
            .build();

        crmActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CrmActivityResponse> getTimeline(Long clientId, Long opportunityId, Pageable pageable) {
        Long companyId = requireCompanyId();
        Page<CrmActivity> page;
        if (opportunityId != null) {
            page = crmActivityRepository.findByCompanyIdAndOpportunityIdOrderByActivityDateDesc(companyId, opportunityId, pageable);
        } else if (clientId != null) {
            page = crmActivityRepository.findByCompanyIdAndClientIdOrderByActivityDateDesc(companyId, clientId, pageable);
        } else {
            page = crmActivityRepository.findByCompanyIdOrderByActivityDateDesc(companyId, pageable);
        }
        return page.map(CrmActivityMapper::toResponse);
    }

    @Override
    public CrmActivityResponse markCompleted(Long id) {
        CrmActivity activity = findOwned(id);
        activity.setCompleted(true);
        return CrmActivityMapper.toResponse(crmActivityRepository.save(activity));
    }

    @Override
    public void delete(Long id) {
        CrmActivity activity = findOwned(id);
        if (activity.isSystemGenerated()) {
            throw new BadRequestException("System-generated timeline entries cannot be deleted");
        }
        activity.softDelete();
        crmActivityRepository.save(activity);
    }

    // NOT_SUPPORTED overrides this class's @Transactional so the provider call
    // isn't inside a transaction - see AiTransactionBoundary. Everything that
    // reads entities (including the lazy client.getUser()) happens inside
    // aiTx.load(), which commits before the AI call.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CrmActivitySummaryResponse summarise(Long clientId, Long opportunityId) {
        Long companyId = requireCompanyId();
        if (clientId == null && opportunityId == null) {
            throw new BadRequestException("Provide a clientId or an opportunityId to summarise");
        }

        String prompt = aiTx.load(() -> {
            String recordType;
            String recordName;
            String stageOrStatus;
            Page<CrmActivity> activities;

            if (opportunityId != null) {
                Opportunity opportunity = opportunityRepository.findByIdAndCompanyId(opportunityId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
                recordType = "Opportunity";
                recordName = opportunity.getName();
                stageOrStatus = opportunity.getStage().name();
                activities = crmActivityRepository.findByCompanyIdAndOpportunityIdOrderByActivityDateDesc(
                    companyId, opportunityId, PageRequest.of(0, 10));
            } else {
                Client client = clientRepository.findByIdAndCompanyId(clientId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
                recordType = "Client";
                recordName = client.getClientCompanyName() != null
                    ? client.getClientCompanyName()
                    : (client.getUser() != null ? client.getUser().getFullName() : "Unknown");
                stageOrStatus = client.getStatus() != null ? client.getStatus().name() : "UNKNOWN";
                activities = crmActivityRepository.findByCompanyIdAndClientIdOrderByActivityDateDesc(
                    companyId, clientId, PageRequest.of(0, 10));
            }

            String activityHistory = activities.stream()
                .map(a -> "- [" + a.getType() + "] " + a.getSubject()
                    + (a.getDescription() != null && !a.getDescription().isBlank() ? ": " + a.getDescription() : ""))
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);

            return CrmActivitySummaryPromptBuilder.builder()
                .setRecordType(recordType)
                .setRecordName(recordName)
                .setStageOrStatus(stageOrStatus)
                .setActivityHistory(activityHistory)
                .build();
        });

        CrmActivitySummaryResponse response = new CrmActivitySummaryResponse();
        response.setSummary(aiService.generateRaw(AiFeature.CRM_ACTIVITY_SUMMARY, prompt));
        return response;
    }

    private CrmActivity findOwned(Long id) {
        return crmActivityRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for current platformuser");
        }
        return companyId;
    }
}
