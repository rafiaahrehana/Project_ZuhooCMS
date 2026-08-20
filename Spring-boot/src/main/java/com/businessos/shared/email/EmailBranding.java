package com.businessos.shared.email;

import com.businessos.modules.company.Company;
import com.businessos.modules.website.WebsiteSettings;
import com.businessos.modules.website.WebsiteSettingsRepository;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailBranding {

    private final WebsiteSettingsRepository websiteSettingsRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailBranding(WebsiteSettingsRepository websiteSettingsRepository) {
        this.websiteSettingsRepository = websiteSettingsRepository;
    }

    public Data getPlatformBranding() {
        return Data.builder()
                .companyId(null)
                .companyName("businessos")
                .logoUrl(frontendUrl + "/images/logo.png")
                .primaryColor("#1e3a5f")
                .build();
    }

    public Data from(Company company) {
        if (company == null) {
            return getPlatformBranding();
        }
        return websiteSettingsRepository.findByCompanyId(company.getId())
                .map(settings -> Data.builder()
                        .companyId(company.getId())
                        .companyName(company.getCompanyName())
                        .logoUrl(settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty() ? settings.getLogoUrl() : (frontendUrl + "/images/logo.png"))
                        .primaryColor(settings.getPrimaryColor() != null && !settings.getPrimaryColor().isEmpty() ? settings.getPrimaryColor() : "#1e3a5f")
                        .build())
                .orElseGet(() -> Data.builder()
                        .companyId(company.getId())
                        .companyName(company.getCompanyName())
                        .logoUrl(frontendUrl + "/images/logo.png")
                        .primaryColor("#1e3a5f")
                        .build());
    }

    @Getter
    @Builder
    public static class Data {
        private Long companyId;
        private String companyName;
        private String logoUrl;
        private String primaryColor;
    }
}