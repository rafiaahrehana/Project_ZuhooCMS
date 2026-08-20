
package com.businessos.modules.company;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.enums.Role;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.enums.CompanyStatus;
import com.businessos.modules.servicedesk.companyservice.CompanyServiceRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.enums.ServiceRequestStatus;
import com.businessos.modules.website.WebsiteSettingsRepository;
import com.businessos.shared.address.AddressMapper;
import com.businessos.shared.subscription.BillingCycle;
import com.businessos.shared.subscription.SubscriptionHistory;
import com.businessos.shared.subscription.SubscriptionHistoryRepository;
import com.businessos.shared.subscription.SubscriptionPlanDefinition;
import com.businessos.shared.subscription.SubscriptionPlanDefinitionRepository;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.audit.AuditService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CompanyServiceRepository hubServiceRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final SubscriptionPlanDefinitionRepository subscriptionPlanDefinitionRepository;
    private final SecurityUtil securityUtil;
    private final WebsiteSettingsRepository websiteSettingsRepository;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final com.businessos.shared.notification.NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public CompanyPublicResponse getBySubdomain(String subdomain) {
        Company company = companyRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with subdomain: " + subdomain));
        CompanyPublicResponse publicResponse = CompanyMapper.toPublicResponse(company);
        applyBranding(publicResponse, company.getId());
        return publicResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CompanyPublicResponse> getPublicList() {
        return companyRepository
                .findByStatusInOrderByCompanyNameAsc(List.of(CompanyStatus.ACTIVE, CompanyStatus.TRIAL))
                .stream()
                .map(CompanyMapper::toPublicResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.businessos.modules.servicedesk.companyservice.CompanyServiceResponse> getPublicServices(String subdomain) {
        Company company = companyRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with subdomain: " + subdomain));
        return hubServiceRepository.findByCompanyIdAndActiveTrue(company.getId())
                .stream()
                .map(com.businessos.modules.servicedesk.companyservice.CompanyServiceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id : " + id));
        CompanyResponse response = CompanyMapper.toResponse(company);
        response.setLocationDetail(addressMapper.toResponse(company.getLocationDetail()));
        applyBranding(response, company.getId());
        return response;
    }

    @Override
    public CompanyResponse update(Long id, UpdateCompanyRequest request) {
        authorizationService.checkAnyPermission(
                PermissionCode.COMPANY_UPDATE, PermissionCode.COMPANY_SETTINGS, PermissionCode.COMPANY_BRANDING);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (request.getCompanyName() != null) company.setCompanyName(request.getCompanyName());
        if (request.getCompanyPhone() != null) company.setCompanyPhone(request.getCompanyPhone());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getPortalAbout() != null) company.setPortalAbout(request.getPortalAbout());
        if (request.getTaxRegistrationNumber() != null) company.setTaxRegistrationNumber(request.getTaxRegistrationNumber());
        if (request.getBankName() != null) company.setBankName(request.getBankName());
        if (request.getBankAccountName() != null) company.setBankAccountName(request.getBankAccountName());
        if (request.getBankAccountNumber() != null) company.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankBranch() != null) company.setBankBranch(request.getBankBranch());
        if (request.getFiscalYearStartMonth() != null) {
            if (request.getFiscalYearStartMonth() < 1 || request.getFiscalYearStartMonth() > 12) {
                throw new BadRequestException("Fiscal year start month must be between 1 and 12");
            }
            company.setFiscalYearStartMonth(request.getFiscalYearStartMonth());
        }
        if (request.getBaseCurrency() != null && !request.getBaseCurrency().isBlank()) {
            company.setBaseCurrency(request.getBaseCurrency().trim().toUpperCase());
        }

        if (request.getLogo() != null || request.getPrimaryColor() != null
                || request.getSecondaryColor() != null || request.getTagline() != null) {
            com.businessos.modules.website.WebsiteSettings settings =
                    websiteSettingsRepository.findByCompanyId(company.getId())
                            .orElseGet(() -> com.businessos.modules.website.WebsiteSettings.builder()
                                    .companyId(company.getId())
                                    .companyName(company.getCompanyName())
                                    .build());
            if (request.getLogo() != null) settings.setLogoUrl(request.getLogo());
            if (request.getPrimaryColor() != null) settings.setPrimaryColor(request.getPrimaryColor());
            if (request.getSecondaryColor() != null) settings.setSecondaryColor(request.getSecondaryColor());
            if (request.getTagline() != null) settings.setTagline(request.getTagline());
            websiteSettingsRepository.save(settings);
        }

        // Structured address - same create-or-update pattern as the user profile
        if (request.getLocationDetail() != null) {
            if (company.getLocationDetail() == null) {
                company.setLocationDetail(addressMapper.toEntity(request.getLocationDetail()));
            } else {
                addressMapper.updateEntityFromRequest(company.getLocationDetail(), request.getLocationDetail());
            }
        }

        Company savedCompany = companyRepository.save(company);
        CompanyResponse response = CompanyMapper.toResponse(savedCompany);
        response.setLocationDetail(addressMapper.toResponse(savedCompany.getLocationDetail()));
        applyBranding(response, savedCompany.getId());
        return response;
    }

    @Override
    public CompanyResponse registerByAdmin(RegisterCompanyRequest request) {
        if (companyRepository.existsBySubdomain(request.getSubdomain())) {
            throw new BadRequestException("Subdomain already exists.");
        }
        if (userRepository.existsByEmail(request.getOwnerEmail())) {
            throw new BadRequestException("Email already exists.");
        }

        User owner = new User();
        owner.setFirstName(request.getOwnerFirstName());
        owner.setLastName(request.getOwnerLastName());
        owner.setEmail(request.getOwnerEmail().toLowerCase().trim());
        owner.setPassword(passwordEncoder.encode(request.getOwnerPassword()));
        owner.setRole(Role.COMPANY_OWNER);
        owner.setPhone(request.getCompanyPhone());
        owner.setActive(true);
        owner = userRepository.save(owner);

        Company company = new Company();
        company.setCompanyName(request.getCompanyName());
        company.setSubdomain(request.getSubdomain());
        company.setCompanyPhone(request.getCompanyPhone());
        company.setCompanyEmail(request.getOwnerEmail().toLowerCase().trim());
        company.setOwner(owner);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setSubscriptionPlan("FREE");
        if (request.getLocationDetail() != null) {
            company.setLocationDetail(addressMapper.toEntity(request.getLocationDetail()));
        }

        company = companyRepository.save(company);
        return CompanyMapper.toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> listAll(CompanyStatus status, String plan, String keyword, Pageable pageable) {
        return companyRepository
            .findFiltered(status, plan, keyword == null ? null : keyword.trim(), pageable)
            .map(CompanyMapper::toResponse);
    }

    @Override
    public CompanyResponse changePlan(Long id, String planCode, BigDecimal amountPaid, String transactionRef) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        SubscriptionPlanDefinition planDef = subscriptionPlanDefinitionRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown plan: " + planCode));

        String fromPlan = company.getSubscriptionPlan();
        LocalDate today = LocalDate.now();
        company.setSubscriptionPlan(planDef.getCode());
        // Admin-assigned plan changes previously never set a subscription window at
        // all (subscriptionEnd stayed whatever it was, often null) - the billing
        // cycle now drives a real one, same as the self-service paid upgrade does.
        company.setSubscriptionStart(today);
        company.setSubscriptionEnd(planDef.getBillingCycle() == BillingCycle.YEARLY
                ? today.plusYears(1) : today.plusMonths(1));
        // findTrialExpiringBetween() permanently excludes any company with this
        // set - without resetting it here, a company reminded once in an earlier
        // cycle would never be reminded again in any future cycle.
        company.setTrialReminderSentAt(null);
        Company saved = companyRepository.save(company);

        subscriptionHistoryRepository.save(SubscriptionHistory.builder()
                .company(saved)
                .fromPlan(fromPlan)
                .toPlan(planDef.getCode())
                .subscriptionStart(saved.getSubscriptionStart())
                .subscriptionEnd(saved.getSubscriptionEnd())
                .amountPaid(amountPaid)
                .transactionRef(transactionRef)
                .changedAt(LocalDateTime.now())
                .changedBy(securityUtil.getCurrentUser())
                .build());

        if (saved.getOwner() != null && !planDef.getCode().equals(fromPlan)) {
            notificationService.send(com.businessos.shared.notification.CreateNotificationRequest.of(
                    com.businessos.enums.NotificationType.GENERAL,
                    "Your plan was changed",
                    "Your company's plan was changed from " + fromPlan + " to " + planDef.getCode()
                            + " by a platform administrator.",
                    "/settings/subscription",
                    saved.getOwner().getId(),
                    id));
        }

        return CompanyMapper.toResponse(saved);
    }

    @Override
    public void applyPaidPlanUpgrade(Long companyId, SubscriptionPlanDefinition plan, BigDecimal amountPaid,
                                     String transactionRef, Long changedByUserId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String fromPlan = company.getSubscriptionPlan();
        LocalDate today = LocalDate.now();
        company.setSubscriptionPlan(plan.getCode());
        company.setSubscriptionStart(today);
        company.setSubscriptionEnd(plan.getBillingCycle() == BillingCycle.YEARLY
                ? today.plusYears(1) : today.plusMonths(1));
        // Same reasoning as changePlan(): without this reset the company would be
        // permanently excluded from future reminder runs after being reminded once.
        company.setTrialReminderSentAt(null);
        company.setActive(true);
        // A paid upgrade should lift the company out of TRIAL/SUSPENDED immediately -
        // ENTERPRISE/PENDING_VERIFICATION-only setups aside, this is the same de-facto
        // "reactivate on payment" behavior SubscriptionScheduler's suspension implies.
        if (company.getStatus() == CompanyStatus.TRIAL || company.getStatus() == CompanyStatus.SUSPENDED
                || company.getStatus() == CompanyStatus.PENDING_VERIFICATION) {
            company.setStatus(CompanyStatus.ACTIVE);
        }
        Company saved = companyRepository.save(company);

        // changedBy comes from the transaction's initiatedByUserId, not SecurityUtil -
        // this runs from a payment gateway callback, which has no security context.
        subscriptionHistoryRepository.save(SubscriptionHistory.builder()
                .company(saved)
                .fromPlan(fromPlan)
                .toPlan(plan.getCode())
                .subscriptionStart(saved.getSubscriptionStart())
                .subscriptionEnd(saved.getSubscriptionEnd())
                .amountPaid(amountPaid)
                .transactionRef(transactionRef)
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUserId != null ? userRepository.findById(changedByUserId).orElse(null) : null)
                .build());
    }

    @Override
    public CompanyResponse changeStatus(Long id, CompanyStatus status) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (status == CompanyStatus.DEACTIVATED || status == CompanyStatus.SUSPENDED) {
            boolean hasActiveRequests = serviceRequestRepository.existsByCompanyIdAndStatusNotIn(
                id, List.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.CANCELLED)
            );
            if (hasActiveRequests) {
                throw new BadRequestException("Cannot change status. There are active service requests.");
            }
        }

        CompanyStatus oldStatus = company.getStatus();
        company.setStatus(status);
        if (status == CompanyStatus.ACTIVE) {
            company.setActive(true);
        } else if (status == CompanyStatus.SUSPENDED || status == CompanyStatus.DEACTIVATED) {
            company.setActive(false);
        }
        CompanyResponse response = CompanyMapper.toResponse(companyRepository.save(company));

        // Suspending/reactivating an entire tenant is one of the highest-blast-radius
        // actions a platform admin can take - it previously left no audit trail at all.
        auditService.log(AuditEntityType.COMPANY, id, AuditAction.UPDATE,
                oldStatus != null ? oldStatus.name() : null, status.name(),
                securityUtil.getCurrentUser(), id, null);

        // Previously the owner found out their company was suspended only when
        // features started disappearing - no email/notification existed on any
        // admin-driven plan or status change.
        if (company.getOwner() != null && oldStatus != status) {
            notificationService.send(com.businessos.shared.notification.CreateNotificationRequest.of(
                    com.businessos.enums.NotificationType.GENERAL,
                    "Your company status changed",
                    "Your company's status was changed to " + status + " by a platform administrator.",
                    "/settings/subscription",
                    company.getOwner().getId(),
                    id));
        }

        return response;
    }

    @Override
    public void deactivate(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        boolean hasActiveRequests = serviceRequestRepository.existsByCompanyIdAndStatusNotIn(
            id, List.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.CANCELLED)
        );
        if (hasActiveRequests) {
            throw new BadRequestException("Cannot deactivate company. There are active service requests.");
        }

        if (employeeRepository.existsByCompanyId(id)) {
            throw new BadRequestException("Cannot delete company with existing employees.");
        }

        if (clientRepository.existsByCompanyId(id)) {
            throw new BadRequestException("Cannot delete company with existing clients.");
        }

        CompanyStatus oldStatus = company.getStatus();
        company.setActive(false);
        company.softDelete();
        companyRepository.save(company);

        if (company.getOwner() != null) {
            company.getOwner().softDelete();
            company.getOwner().setActive(false);
            userRepository.save(company.getOwner());
        }

        auditService.log(AuditEntityType.COMPANY, id, AuditAction.UPDATE,
                oldStatus != null ? oldStatus.name() : null, "DEACTIVATED",
                securityUtil.getCurrentUser(), id, null);
    }

    // Overrides the legacy Company branding columns with the authoritative
    // values from WebsiteSettings when a settings row exists.
    private void applyBranding(CompanyResponse response, Long companyId) {
        websiteSettingsRepository.findByCompanyId(companyId).ifPresent(settings -> {
            if (settings.getLogoUrl() != null) response.setLogo(settings.getLogoUrl());
            if (settings.getPrimaryColor() != null) response.setPrimaryColor(settings.getPrimaryColor());
            if (settings.getSecondaryColor() != null) response.setSecondaryColor(settings.getSecondaryColor());
            if (settings.getTagline() != null) response.setTagline(settings.getTagline());
        });
    }

    private void applyBranding(CompanyPublicResponse response, Long companyId) {
        websiteSettingsRepository.findByCompanyId(companyId).ifPresent(settings -> {
            if (settings.getLogoUrl() != null) response.setLogo(settings.getLogoUrl());
            if (settings.getPrimaryColor() != null) response.setPrimaryColor(settings.getPrimaryColor());
            if (settings.getSecondaryColor() != null) response.setSecondaryColor(settings.getSecondaryColor());
            if (settings.getTagline() != null) response.setTagline(settings.getTagline());
        });
    }
}
