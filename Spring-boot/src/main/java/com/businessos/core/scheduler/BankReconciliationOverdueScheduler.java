package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.reconciliation.BankReconciliation;
import com.businessos.modules.finance.reconciliation.BankReconciliationRepository;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Bank reconciliations that stay pending only surfaced when someone happened to visit
 * the Bank Reconciliation page (getPendingReconciliations was never proactively pushed
 * anywhere). This runs daily and notifies the company owner once a reconciliation has
 * been sitting unreconciled for OVERDUE_DAYS - mirrors the InvoiceOverdueScheduler /
 * LicenseExpiryScheduler pattern already used elsewhere.
 */
@Component
@RequiredArgsConstructor
public class BankReconciliationOverdueScheduler {

    private static final int OVERDUE_DAYS = 7;

    private final BankReconciliationRepository reconciliationRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void flagOverdueReconciliations() {
        LocalDate threshold = LocalDate.now().minusDays(OVERDUE_DAYS);

        for (Company company : companyRepository.findAll()) {
            if (company.isPlatformTenant() || company.getOwner() == null) continue;

            List<BankReconciliation> overdue = reconciliationRepository
                    .findByCompanyIdAndReconciledFalseAndReconciliationDate(company.getId(), threshold);

            for (BankReconciliation reconciliation : overdue) {
                String accountName = reconciliation.getBankAccount() != null
                        ? reconciliation.getBankAccount().getAccountName() : "a bank account";
                notificationService.send(CreateNotificationRequest.of(
                        NotificationType.RECONCILIATION_OVERDUE,
                        "Bank reconciliation overdue",
                        "The reconciliation for " + accountName + " dated " + reconciliation.getReconciliationDate()
                                + " has been pending for " + OVERDUE_DAYS + " days.",
                        "/finance/bank-reconciliation",
                        company.getOwner().getId(),
                        company.getId()));
            }
        }
    }
}
