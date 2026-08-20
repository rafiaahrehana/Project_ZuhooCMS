package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.asset.Asset;
import com.businessos.modules.hrm.asset.AssetRepository;
import com.businessos.shared.email.EmailService;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mirrors LicenseExpiryScheduler for hardware: nobody should have to remember
 * to check each asset's warranty date by hand. Runs daily and notifies the
 * company owner exactly once per asset per state (expiring soon / expired),
 * using warrantyExpiringSoonAlertedAt/warrantyExpiredAlertedAt as the
 * already-notified marker since AssetStatus has no warranty-specific states
 * to auto-transition the way SoftwareLicense's status does.
 */
@Component
@RequiredArgsConstructor
public class WarrantyExpiryScheduler {

    private static final int EXPIRING_SOON_WINDOW_DAYS = 30;

    private final AssetRepository assetRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processWarrantyExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(EXPIRING_SOON_WINDOW_DAYS);

        List<Asset> newlyExpiringSoon = assetRepository.findNewlyWarrantyExpiringSoon(today, cutoff);
        assetRepository.bulkMarkWarrantyExpiringSoonAlerted(today, cutoff);
        for (Asset asset : newlyExpiringSoon) {
            alertOwner(asset, NotificationType.WARRANTY_EXPIRING);
        }

        List<Asset> newlyExpired = assetRepository.findNewlyWarrantyExpired(today);
        assetRepository.bulkMarkWarrantyExpiredAlerted(today);
        for (Asset asset : newlyExpired) {
            alertOwner(asset, NotificationType.WARRANTY_EXPIRED);
        }
    }

    private void alertOwner(Asset asset, NotificationType type) {
        Company company = companyRepository.findById(asset.getCompany().getId()).orElse(null);
        if (company == null || company.getOwner() == null) return;

        String ownerEmail = company.getOwner().getEmail();
        String ownerName = company.getOwner().getFirstName();
        String assetLabel = asset.getName() + (asset.getAssetTag() != null ? " (" + asset.getAssetTag() + ")" : "");

        if (type == NotificationType.WARRANTY_EXPIRING) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), asset.getWarrantyExpiry());
            notificationService.send(CreateNotificationRequest.of(
                    type, "Hardware warranty expiring soon",
                    assetLabel + " warranty expires on " + asset.getWarrantyExpiry()
                            + " (" + daysLeft + " days) - arrange coverage before it lapses.",
                    "/itam/hardware", company.getOwner().getId(), company.getId()));
            emailService.sendWarrantyExpiryReminder(ownerEmail, ownerName, assetLabel, asset.getWarrantyExpiry(), daysLeft);
        } else {
            notificationService.send(CreateNotificationRequest.of(
                    type, "Hardware warranty expired",
                    assetLabel + " warranty expired on " + asset.getWarrantyExpiry() + ".",
                    "/itam/hardware", company.getOwner().getId(), company.getId()));
            emailService.sendWarrantyExpiredEmail(ownerEmail, ownerName, assetLabel, asset.getWarrantyExpiry());
        }
    }
}
