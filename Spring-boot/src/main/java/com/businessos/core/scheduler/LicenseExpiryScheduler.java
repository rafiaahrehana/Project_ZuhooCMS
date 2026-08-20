package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.itam.software.LicenseStatus;
import com.businessos.modules.itam.software.SoftwareLicense;
import com.businessos.modules.itam.software.SoftwareLicenseRepository;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Companies shouldn't have to rely on the vendor's own renewal email to notice
 * a software license is about to lapse. This runs daily, auto-transitions
 * license status as expiry approaches/passes, and notifies the company owner
 * the moment a license newly crosses into each state (not every day after).
 *
 * ACTIVE -> EXPIRING_SOON: within 30 days of licenseExpiryDate.
 * ACTIVE/EXPIRING_SOON -> EXPIRED: past licenseExpiryDate.
 */
@Component
@RequiredArgsConstructor
public class LicenseExpiryScheduler {

    private static final int EXPIRING_SOON_WINDOW_DAYS = 30;
    private static final List<LicenseStatus> ACTIVE_STATUSES =
            List.of(LicenseStatus.ACTIVE, LicenseStatus.EXPIRING_SOON);

    private final SoftwareLicenseRepository licenseRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processLicenseExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(EXPIRING_SOON_WINDOW_DAYS);

        List<SoftwareLicense> newlyExpiringSoon = licenseRepository
                .findNewlyEnteringExpiringSoon(LicenseStatus.ACTIVE, today, cutoff);
        licenseRepository.bulkMarkExpiringSoon(LicenseStatus.EXPIRING_SOON, LicenseStatus.ACTIVE, today, cutoff);

        for (SoftwareLicense license : newlyExpiringSoon) {
            alertOwner(license, NotificationType.LICENSE_EXPIRING);
        }

        List<SoftwareLicense> newlyExpired = licenseRepository.findNewlyExpired(ACTIVE_STATUSES, today);
        licenseRepository.bulkMarkExpired(LicenseStatus.EXPIRED, ACTIVE_STATUSES, today);

        for (SoftwareLicense license : newlyExpired) {
            alertOwner(license, NotificationType.LICENSE_EXPIRED);
        }
    }

    private void alertOwner(SoftwareLicense license, NotificationType type) {
        Company company = companyRepository.findById(license.getCompanyId()).orElse(null);
        if (company == null || company.getOwner() == null) return;

        String ownerEmail = company.getOwner().getEmail();
        String ownerName = company.getOwner().getFirstName();

        if (type == NotificationType.LICENSE_EXPIRING) {
            notificationService.send(CreateNotificationRequest.of(
                    type, "Software license expiring soon",
                    license.getSoftwareName() + " expires on " + license.getLicenseExpiryDate()
                            + " (" + license.getDaysUntilExpiry() + " days) - renew or cancel before it lapses.",
                    "/itam/software", company.getOwner().getId(), company.getId()));
            emailService.sendLicenseExpiryReminder(
                    ownerEmail, ownerName, license.getSoftwareName(),
                    license.getLicenseExpiryDate(), license.getDaysUntilExpiry());
        } else {
            notificationService.send(CreateNotificationRequest.of(
                    type, "Software license expired",
                    license.getSoftwareName() + " expired on " + license.getLicenseExpiryDate()
                            + " - " + license.getSeatsUsed() + " assigned seat(s) may lose access.",
                    "/itam/software", company.getOwner().getId(), company.getId()));
            emailService.sendLicenseExpiredEmail(
                    ownerEmail, ownerName, license.getSoftwareName(),
                    license.getLicenseExpiryDate(), license.getSeatsUsed());
        }
    }
}
