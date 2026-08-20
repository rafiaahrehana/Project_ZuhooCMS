package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.crm.activity.CrmActivity;
import com.businessos.modules.crm.activity.CrmActivityRepository;
import com.businessos.modules.crm.lead.Lead;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.enums.LeadStatus;
import com.businessos.modules.crm.opportunity.Opportunity;
import com.businessos.modules.crm.opportunity.OpportunityRepository;
import com.businessos.modules.crm.opportunity.OpportunityStage;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Wires up CrmActivity.followUpAt/followUpDone, which were previously persisted but
 * never read anywhere. Does not auto-mark followUpDone - the rep completes it manually
 * (e.g. from the Dashboard's Upcoming Follow-ups list), this only notifies once it's due.
 */
@Component
@RequiredArgsConstructor
public class CrmFollowUpScheduler {

    private final CrmActivityRepository crmActivityRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final NotificationService notificationService;

    private static final List<LeadStatus> LEAD_CLOSED_STATUSES = List.of(LeadStatus.DISQUALIFIED);
    private static final List<OpportunityStage> OPP_CLOSED_STAGES =
            List.of(OpportunityStage.WON, OpportunityStage.LOST);

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void notifyDueFollowUps() {
        LocalDateTime now = LocalDateTime.now();
        // The notified-at filter is what makes this fire once per follow-up.
        // Without it, every overdue item re-notified on every run - 48
        // identical notifications a day until someone marked it done.
        List<CrmActivity> due = crmActivityRepository
                .findByFollowUpAtLessThanEqualAndFollowUpDoneFalseAndFollowUpNotifiedAtIsNullAndDeletedFalse(now);

        for (CrmActivity activity : due) {
            // Stamped before the null-recipient skip, or an ownerless follow-up
            // would be re-scanned forever.
            activity.setFollowUpNotifiedAt(now);

            if (activity.getPerformedBy() == null) continue;

            String subject = activity.getLead() != null ? activity.getLead().getContactName()
                    : activity.getOpportunity() != null ? activity.getOpportunity().getName()
                    : activity.getClient() != null ? activity.getClient().getClientCompanyName()
                    : activity.getSubject();

            // Land the user on the record the follow-up is about, not a fixed page.
            String link = activity.getOpportunity() != null ? "/crm/pipeline"
                    : activity.getClient() != null ? "/crm/clients"
                    : "/crm/leads";

            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.FOLLOW_UP_DUE,
                    "Follow-up due",
                    "Follow-up \"" + activity.getSubject() + "\" for " + subject + " is due.",
                    link,
                    activity.getPerformedBy().getId(),
                    activity.getCompany().getId()
            ));
        }
    }

    // The "Stale" tab and dashboard widget both already query these exact
    // conditions (LeadRepository.findStalLeads / OpportunityRepository
    // .findStaleOpenOpportunities) - nothing ever proactively told the rep or
    // owner a lead/deal had actually crossed into that state. Daily, not every
    // 30 minutes: staleness is a slow-moving signal, unlike a follow-up's exact
    // due timestamp.
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void notifyStalePipeline() {
        LocalDate leadCutoff = LocalDate.now().minusDays(30);
        for (Lead lead : leadRepository.findNewlyStaleLeads(leadCutoff, LEAD_CLOSED_STATUSES)) {
            lead.setStaleNotifiedAt(LocalDateTime.now());
            if (lead.getAssignedTo() == null || lead.getAssignedTo().getUser() == null) continue;
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.FOLLOW_UP_DUE,
                    "Lead has gone stale",
                    lead.getContactName() + " hasn't been contacted since " + lead.getLastContactDate()
                            + " - it may need a follow-up.",
                    "/crm/leads",
                    lead.getAssignedTo().getUser().getId(),
                    lead.getCompany().getId()
            ));
        }

        LocalDateTime oppCutoff = LocalDateTime.now().minusDays(14);
        for (Opportunity opportunity : opportunityRepository.findNewlyStaleOpportunities(OPP_CLOSED_STAGES, oppCutoff)) {
            opportunity.setStaleNotifiedAt(LocalDateTime.now());
            if (opportunity.getOwner() == null || opportunity.getOwner().getUser() == null) continue;
            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.FOLLOW_UP_DUE,
                    "Deal has gone stale",
                    opportunity.getName() + " has had no activity in over 14 days - it may be going cold.",
                    "/crm/pipeline",
                    opportunity.getOwner().getUser().getId(),
                    opportunity.getCompany().getId()
            ));
        }
    }
}
