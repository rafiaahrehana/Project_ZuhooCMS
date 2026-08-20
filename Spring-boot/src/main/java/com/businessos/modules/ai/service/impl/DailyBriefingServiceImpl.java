package com.businessos.modules.ai.service.impl;

import com.businessos.auth.user.User;
import com.businessos.modules.ai.entity.AiDailyBriefing;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.DailyBriefingPromptBuilder;
import com.businessos.modules.ai.repository.AiDailyBriefingRepository;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.ai.service.DailyBriefingService;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import com.businessos.modules.ai.support.PreparedPrompt;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.announcement.AnnouncementService;
import com.businessos.modules.hrm.leave.LeaveService;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceResponse;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetService;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetResponse;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestResponse;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DailyBriefingServiceImpl implements DailyBriefingService {

    private final AiDailyBriefingRepository briefingRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;
    private final ServiceRequestService serviceRequestService;
    private final AnnouncementService announcementService;
    private final TimesheetService timesheetService;
    private final LeaveService leaveService;

    @Override
    public String getOrBuildToday() {
        User user = securityUtil.getCurrentUser();
        Long companyId = securityUtil.getCurrentCompanyId();
        LocalDate today = LocalDate.now();

        return briefingRepository.findByCompanyIdAndUserIdAndBriefingDate(companyId, user.getId(), today)
            .map(AiDailyBriefing::getContent)
            .orElseGet(() -> build(user, companyId, today));
    }

    private String build(User user, Long companyId, LocalDate today) {
        PreparedPrompt<Void> prepared = aiTx.load(() -> {
            List<ServiceRequestResponse> assigned = serviceRequestService
                .listAssignedToMe(PageRequest.of(0, 50)).getContent();
            long slaBreached = assigned.stream().filter(ServiceRequestResponse::isSlaBreach).count();

            int activeAnnouncements = announcementService.listActive().size();

            double weekHours = timesheetService.listMine(PageRequest.of(0, 50)).getContent().stream()
                .filter(t -> isInCurrentWeek(t.getWorkDate()))
                .mapToDouble(TimesheetResponse::getHoursWorked)
                .sum();

            String lowBalanceNote = null;
            try {
                List<LeaveBalanceResponse> balances = leaveService.getMyBalances(today.getYear());
                lowBalanceNote = balances.stream()
                    .filter(b -> b.getRemainingDays() <= 2)
                    .map(b -> b.getLeaveType() + ": only " + b.getRemainingDays() + " day(s) left")
                    .reduce((a, b) -> a + "; " + b)
                    .orElse(null);
            } catch (Exception ignored) {
                // Leave balances aren't set up for every company - a briefing
                // shouldn't fail just because this one nudge isn't available.
            }

            return new PreparedPrompt<Void>(null, DailyBriefingPromptBuilder.builder()
                .setEmployeeFirstName(user.getFirstName() != null ? user.getFirstName() : "there")
                .setAssignedServiceRequestCount(assigned.size())
                .setSlaBreachedCount((int) slaBreached)
                .setActiveAnnouncementCount(activeAnnouncements)
                .setWeekHoursLogged(weekHours)
                .setLowLeaveBalanceNote(lowBalanceNote)
                .build());
        });

        String content = aiService.generateRaw(AiFeature.DAILY_BRIEFING, prepared.prompt()).trim();

        aiTx.persist(() -> {
            Company company = companyRepository.getReferenceById(companyId);
            briefingRepository.save(AiDailyBriefing.builder()
                .briefingDate(today)
                .content(content)
                .company(company)
                .user(user)
                .build());
            return null;
        });

        return content;
    }

    private boolean isInCurrentWeek(LocalDate date) {
        if (date == null) return false;
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate now = LocalDate.now();
        return date.get(wf.weekOfWeekBasedYear()) == now.get(wf.weekOfWeekBasedYear())
            && date.get(wf.weekBasedYear()) == now.get(wf.weekBasedYear());
    }
}
