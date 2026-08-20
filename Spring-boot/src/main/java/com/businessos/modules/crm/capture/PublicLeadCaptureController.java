package com.businessos.modules.crm.capture;

import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.NotificationType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.lead.Lead;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.modules.website.WebsiteService;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Anonymous lead capture - the endpoint the marketing site's contact form and
 * tenant portal contact forms post to.
 *
 * Unauthenticated by design (see PUBLIC_ENDPOINTS in SecurityConfig), which
 * shapes everything about it:
 *
 *  - It always answers with the same generic success body. Field-level errors
 *    from validation are fine, but "this email already exists" is not - that
 *    would let anyone probe which addresses are in a company's CRM.
 *  - Duplicate submissions (same email or phone, same company) are silently
 *    accepted and dropped rather than creating a second lead: the repeat
 *    visitor pressing submit twice should not double the pipeline.
 *  - A honeypot field swallows the bulk of dumb form spam. It is not real rate
 *    limiting; put the endpoint behind one (e.g. AWS WAF) in production.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/crm")
public class PublicLeadCaptureController {

    private final LeadRepository leadRepository;
    private final CompanyRepository companyRepository;
    private final WebsiteService websiteService;
    private final NotificationService notificationService;

    /**
     * Which company receives leads from the PLATFORM's own landing page (tenant
     * portals are resolved by subdomain instead). Unset means the landing-page
     * form accepts submissions but has nowhere to put them, which is logged
     * loudly rather than failed loudly - the visitor is not the right audience
     * for a configuration error.
     */
    @Value("${app.crm.platform-lead-company-id:}")
    private String platformLeadCompanyId;

    @PostMapping("/leads")
    @Transactional
    public ResponseEntity<Map<String, String>> capture(@Valid @RequestBody PublicLeadRequest request) {
        // Bots fill every field; humans never see this one.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            return ok();
        }
        // Email or phone - a lead reachable by neither is not a lead.
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().isBlank();
        if (!hasEmail && !hasPhone) {
            return ok(); // indistinguishable from success, on purpose
        }

        Company company = resolveTargetCompany(request.getSubdomain());
        if (company == null) {
            log.warn("Public lead dropped - no target company (subdomain={}, platform id configured={})",
                    request.getSubdomain(), !platformLeadCompanyId.isBlank());
            return ok();
        }

        // Same-company dedupe; the visitor is told nothing either way.
        if (hasEmail && leadRepository.existsByEmailAndCompanyIdAndDeletedFalse(request.getEmail().trim(), company.getId())) {
            return ok();
        }
        if (hasPhone && leadRepository.existsByPhoneAndCompanyIdAndDeletedFalse(request.getPhone().trim(), company.getId())) {
            return ok();
        }

        Lead lead = new Lead();
        lead.setContactName(request.getName().trim());
        lead.setCompanyName(trimOrNull(request.getCompanyName()));
        lead.setEmail(hasEmail ? request.getEmail().trim() : null);
        lead.setPhone(hasPhone ? request.getPhone().trim() : null);
        lead.setNotes(trimOrNull(request.getMessage()));
        lead.setStatus(LeadStatus.NEW);
        lead.setSource(LeadSource.WEBSITE);
        lead.setCompany(company);
        leadRepository.save(lead);

        notifyOwner(company, lead);
        return ok();
    }

    private Company resolveTargetCompany(String subdomain) {
        if (subdomain != null && !subdomain.isBlank()) {
            try {
                Long id = websiteService.resolveCompanyId(null, subdomain);
                return companyRepository.findById(id).orElse(null);
            } catch (Exception e) {
                return null; // unknown subdomain - drop silently, do not confirm which subdomains exist
            }
        }
        if (platformLeadCompanyId.isBlank()) return null;
        try {
            return companyRepository.findById(Long.parseLong(platformLeadCompanyId.trim())).orElse(null);
        } catch (NumberFormatException e) {
            log.warn("app.crm.platform-lead-company-id is not a number: {}", platformLeadCompanyId);
            return null;
        }
    }

    /** New inbound lead has no assignee yet, so the company owner is told. */
    private void notifyOwner(Company company, Lead lead) {
        if (company.getOwner() == null) return;
        notificationService.send(CreateNotificationRequest.of(
                NotificationType.LEAD_ASSIGNED,
                "New website lead",
                "\"" + lead.getContactName() + "\" reached out via the website"
                        + (lead.getEmail() != null ? " (" + lead.getEmail() + ")" : "") + ".",
                "/crm/leads",
                company.getOwner().getId(),
                company.getId()
        ));
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ResponseEntity<Map<String, String>> ok() {
        return ResponseEntity.ok(Map.of("message", "Thanks - we will be in touch shortly."));
    }
}
