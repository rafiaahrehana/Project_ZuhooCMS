package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationRepository;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.businessos.enums.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor

public class InvoiceOverdueScheduler {

    private final ClientInvoiceRepository invoiceRepository;
    private final NotificationRepository  notificationRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    private static final List<InvoiceStatus> OVERDUE_ELIGIBLE =
            List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID);

    /**
     * Marks ISSUED and PARTIALLY_PAID invoices as OVERDUE when their dueDate has passed,
     * and notifies each company's owner once per invoice (not every day it stays
     * overdue - findNewlyOverdue() is queried before the bulk UPDATE flips the status).
     * Runs daily at 01:30.
     */
    @Scheduled(cron = "0 30 1 * * *")
    @Transactional
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<ClientInvoice> newlyOverdue = invoiceRepository.findNewlyOverdue(today, OVERDUE_ELIGIBLE);

        invoiceRepository.markOverdueInvoices(today, InvoiceStatus.OVERDUE, OVERDUE_ELIGIBLE);

        for (ClientInvoice invoice : newlyOverdue) {
            notifyOwner(invoice);
        }
    }

    private void notifyOwner(ClientInvoice invoice) {
        Company company = companyRepository.findById(invoice.getCompanyId()).orElse(null);
        if (company == null || company.getOwner() == null) return;

        notificationService.send(CreateNotificationRequest.of(
                NotificationType.PAYMENT_DUE,
                "Invoice overdue",
                "Invoice " + invoice.getInvoiceNumber() + " (" + invoice.getBalanceAmount()
                        + " outstanding) was due " + invoice.getDueDate() + " and is now overdue.",
                "/finance/invoices",
                company.getOwner().getId(),
                company.getId()));
    }

    /**
     * Cleans up read notifications older than 90 days.
     * Runs monthly on the 1st at 03:30.
     */
    @Scheduled(cron = "0 30 3 1 * *")
    @Transactional
    public void cleanOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int count = notificationRepository.deleteReadOlderThan(cutoff);
        if (count > 0) {
            
        }
    }
}
