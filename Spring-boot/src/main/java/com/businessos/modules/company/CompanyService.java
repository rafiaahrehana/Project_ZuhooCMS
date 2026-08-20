package com.businessos.modules.company;

import com.businessos.enums.CompanyStatus;
import com.businessos.shared.subscription.SubscriptionPlanDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CompanyService {
    CompanyPublicResponse getBySubdomain(String subdomain);

    /** Companies a prospective client can pick from during public client registration. */
    java.util.List<CompanyPublicResponse> getPublicList();

    java.util.List<com.businessos.modules.servicedesk.companyservice.CompanyServiceResponse> getPublicServices(String subdomain);
    CompanyResponse getById(Long id);
    CompanyResponse update(Long id, UpdateCompanyRequest request);
    CompanyResponse registerByAdmin(RegisterCompanyRequest request);
    Page<CompanyResponse> listAll(CompanyStatus status, String plan, String keyword, Pageable pageable);

    /** planCode must match a SubscriptionPlanDefinition.code - its billingCycle
     * determines whether subscriptionEnd becomes +1 month or +1 year from today. */
    CompanyResponse changePlan(Long id, String planCode, BigDecimal amountPaid, String transactionRef);

    /**
     * Applies a plan upgrade the Company Owner paid for through the online checkout
     * (SslCommerzServiceImpl.handleSuccess). Unlike the admin-only changePlan() above,
     * this also (re)activates the subscription window and status, since a paid upgrade
     * should immediately lift a TRIAL/SUSPENDED company back to ACTIVE.
     */
    void applyPaidPlanUpgrade(Long companyId, SubscriptionPlanDefinition plan, BigDecimal amountPaid,
                              String transactionRef, Long changedByUserId);

    CompanyResponse changeStatus(Long id, CompanyStatus status);
    void deactivate(Long id);
}
