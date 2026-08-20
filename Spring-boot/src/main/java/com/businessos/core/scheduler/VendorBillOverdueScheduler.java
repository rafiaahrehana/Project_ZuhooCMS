package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.vendor.VendorBill;
import com.businessos.modules.finance.vendor.VendorBillRepository;
import com.businessos.modules.finance.vendor.VendorBillStatus;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * The mirror image of InvoiceOverdueScheduler for the payables side. Vendor
 * bills previously had no OVERDUE status and no automation at all - a bill
 * 30 days late sat silently in APPROVED/PARTIALLY_PAID with nothing to flag
 * it short of someone manually opening the AP Ageing report.
 */
@Component
@RequiredArgsConstructor
public class VendorBillOverdueScheduler {

    private final VendorBillRepository billRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    private static final List<VendorBillStatus> OVERDUE_ELIGIBLE =
            List.of(VendorBillStatus.APPROVED, VendorBillStatus.PARTIALLY_PAID);

    /** Runs daily at 01:45, offset from the client-invoice job at 01:30. */
    @Scheduled(cron = "0 45 1 * * *")
    @Transactional
    public void markOverdueBills() {
        LocalDate today = LocalDate.now();
        List<VendorBill> newlyOverdue = billRepository.findNewlyOverdueBills(today, OVERDUE_ELIGIBLE);

        billRepository.markOverdueBills(today, VendorBillStatus.OVERDUE, OVERDUE_ELIGIBLE);

        for (VendorBill bill : newlyOverdue) {
            notifyOwner(bill);
        }
    }

    private void notifyOwner(VendorBill bill) {
        Company company = companyRepository.findById(bill.getCompanyId()).orElse(null);
        if (company == null || company.getOwner() == null) return;

        notificationService.send(CreateNotificationRequest.of(
                NotificationType.PAYMENT_DUE,
                "Vendor bill overdue",
                "Bill " + bill.getBillNumber() + " from " + bill.getVendor().getName()
                        + " (" + bill.getBalanceAmount() + " outstanding) was due " + bill.getDueDate()
                        + " and is now overdue.",
                "/finance/vendor-bills",
                company.getOwner().getId(),
                company.getId()));
    }
}
