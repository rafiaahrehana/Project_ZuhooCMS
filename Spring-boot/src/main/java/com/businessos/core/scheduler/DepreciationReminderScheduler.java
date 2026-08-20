package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.fixedasset.DepreciationRunRepository;
import com.businessos.modules.finance.fixedasset.FixedAssetRepository;
import com.businessos.modules.finance.fixedasset.FixedAssetStatus;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * FixedAssetService.runDepreciation() is manually triggered and idempotent per
 * month - nothing ever prompted anyone to actually run it. Six months could pass
 * with book value never reflecting depreciation expense, with nothing warning
 * that prior-period financials were wrong in the interim. This only reminds -
 * running depreciation posts real GL entries, so it stays an explicit human
 * action (FIXED_ASSET_MANAGE), same as every other financial posting in this
 * codebase.
 */
@Component
@RequiredArgsConstructor
public class DepreciationReminderScheduler {

    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationRunRepository depreciationRunRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    // 9am on the 5th of each month - gives a few days into the new month before
    // nagging about the previous one closing out.
    @Scheduled(cron = "0 0 9 5 * *")
    @Transactional(readOnly = true)
    public void remindUnrunDepreciation() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        List<Long> companyIds = fixedAssetRepository.findDistinctCompanyIdsByStatus(FixedAssetStatus.ACTIVE);

        for (Long companyId : companyIds) {
            if (depreciationRunRepository.existsByCompanyIdAndYearAndMonth(
                    companyId, previousMonth.getYear(), previousMonth.getMonthValue())) {
                continue;
            }
            Company company = companyRepository.findById(companyId).orElse(null);
            if (company == null || company.getOwner() == null) continue;

            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.DEPRECIATION_DUE,
                    "Depreciation not yet run for " + previousMonth,
                    "Fixed-asset depreciation for " + previousMonth
                            + " hasn't been run yet - book values won't reflect it until you do.",
                    "/finance/fixed-assets",
                    company.getOwner().getId(),
                    company.getId()));
        }
    }
}
