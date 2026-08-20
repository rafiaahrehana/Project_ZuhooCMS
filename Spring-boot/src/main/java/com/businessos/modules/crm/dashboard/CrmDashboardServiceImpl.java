package com.businessos.modules.crm.dashboard;

import com.businessos.enums.LeadStatus;
import com.businessos.modules.crm.activity.CrmActivity;
import com.businessos.modules.crm.activity.CrmActivityRepository;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.modules.crm.opportunity.OpportunityRepository;
import com.businessos.modules.crm.opportunity.OpportunityStage;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Standalone CRM dashboard summary - deliberately separate from DashboardController/
 * DashboardServiceImpl (the global company dashboard), so CRM gets its own lightweight
 * KPI set instead of being folded into the existing widget-registry framework.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmDashboardServiceImpl implements CrmDashboardService {

    private final OpportunityRepository opportunityRepository;
    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final CrmActivityRepository crmActivityRepository;
    private final SecurityUtil securityUtil;

    @Override
    public CrmDashboardSummaryResponse getSummary() {
        Long companyId = requireCompanyId();

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());

        long totalLeads = leadRepository.countByCompanyId(companyId);
        long convertedLeads = leadRepository.countByCompanyIdAndConvertedTrue(companyId);
        double conversionRate = totalLeads == 0 ? 0.0 : (convertedLeads * 100.0) / totalLeads;

        List<CrmActivity> upcoming = crmActivityRepository
                .findByCompanyIdAndFollowUpDoneFalseAndFollowUpAtGreaterThanEqualOrderByFollowUpAtAsc(
                        companyId, LocalDateTime.now())
                .stream()
                .limit(5)
                .toList();

        List<OpportunityStage> closedStages = List.of(OpportunityStage.WON, OpportunityStage.LOST);

        return CrmDashboardSummaryResponse.builder()
                .pipelineValue(opportunityRepository.sumOpenPipelineValue(companyId))
                .wonThisMonth(opportunityRepository.sumWonAmountBetween(companyId, monthStart, today))
                .qualifiedLeadsCount(leadRepository.countByCompanyIdAndStatusAndConvertedFalse(companyId, LeadStatus.QUALIFIED))
                .conversionRate(Math.round(conversionRate * 10.0) / 10.0)
                .upcomingFollowUps(upcoming.stream().map(this::toUpcomingFollowUp).toList())
                .totalClients(clientRepository.countByCompanyId(companyId))
                .totalLeads(totalLeads)
                .totalOpportunities(opportunityRepository.countByCompanyId(companyId))
                .openOpportunitiesCount(opportunityRepository.countByCompanyIdAndStageNotIn(companyId, closedStages))
                .wonCount(opportunityRepository.countByCompanyIdAndStage(companyId, OpportunityStage.WON))
                .wonValue(opportunityRepository.sumAmountByCompanyIdAndStage(companyId, OpportunityStage.WON))
                .lostCount(opportunityRepository.countByCompanyIdAndStage(companyId, OpportunityStage.LOST))
                .lostValue(opportunityRepository.sumAmountByCompanyIdAndStage(companyId, OpportunityStage.LOST))
                .stageFunnel(stageFunnel(companyId))
                .leadSources(leadSources(companyId))
                .recentDeals(recentDeals(companyId))
                .build();
    }

    /** Open-stage funnel: count, value and share of open pipeline value. */
    private List<CrmDashboardSummaryResponse.StageSlice> stageFunnel(Long companyId) {
        List<CrmDashboardSummaryResponse.StageSlice> slices = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.Map<OpportunityStage, java.math.BigDecimal> values = new java.util.EnumMap<>(OpportunityStage.class);
        for (OpportunityStage stage : OpportunityStage.values()) {
            if (stage == OpportunityStage.WON || stage == OpportunityStage.LOST) continue;
            java.math.BigDecimal value = orZero(opportunityRepository.sumAmountByCompanyIdAndStage(companyId, stage));
            values.put(stage, value);
            total = total.add(value);
        }
        for (OpportunityStage stage : OpportunityStage.values()) {
            if (stage == OpportunityStage.WON || stage == OpportunityStage.LOST) continue;
            java.math.BigDecimal value = values.get(stage);
            slices.add(CrmDashboardSummaryResponse.StageSlice.builder()
                    .stage(stage.name())
                    .count(opportunityRepository.countByCompanyIdAndStage(companyId, stage))
                    .value(value)
                    .percent(total.signum() > 0
                            ? value.multiply(java.math.BigDecimal.valueOf(100))
                                .divide(total, 0, java.math.RoundingMode.HALF_UP).intValue()
                            : 0)
                    .build());
        }
        return slices;
    }

    private List<CrmDashboardSummaryResponse.SourceSlice> leadSources(Long companyId) {
        return java.util.Arrays.stream(com.businessos.enums.LeadSource.values())
                .map(s -> CrmDashboardSummaryResponse.SourceSlice.builder()
                        .source(s.name())
                        .count(leadRepository.countByCompanyIdAndSource(companyId, s))
                        .build())
                .filter(s -> s.getCount() > 0)
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .toList();
    }

    private List<CrmDashboardSummaryResponse.DealItem> recentDeals(Long companyId) {
        return opportunityRepository.findTop5ByCompanyIdOrderByUpdatedAtDesc(companyId).stream()
                .map(o -> CrmDashboardSummaryResponse.DealItem.builder()
                        .id(o.getId())
                        .name(o.getName())
                        .clientName(o.getClient() != null ? o.getClient().getClientCompanyName() : null)
                        .amount(o.getAmount())
                        .stage(o.getStage() != null ? o.getStage().name() : null)
                        .build())
                .toList();
    }

    private java.math.BigDecimal orZero(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }

    private CrmDashboardSummaryResponse.UpcomingFollowUp toUpcomingFollowUp(CrmActivity activity) {
        String relatedName = activity.getLead() != null ? activity.getLead().getContactName()
                : activity.getOpportunity() != null ? activity.getOpportunity().getName()
                : activity.getClient() != null ? activity.getClient().getClientCompanyName()
                : null;
        return CrmDashboardSummaryResponse.UpcomingFollowUp.builder()
                .activityId(activity.getId())
                .subject(activity.getSubject())
                .followUpAt(activity.getFollowUpAt())
                .relatedName(relatedName)
                .build();
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context found");
        }
        return companyId;
    }
}
