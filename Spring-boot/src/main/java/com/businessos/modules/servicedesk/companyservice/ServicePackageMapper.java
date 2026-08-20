package com.businessos.modules.servicedesk.companyservice;

import com.businessos.modules.crm.client.Client;
import com.businessos.auth.user.User;

import java.util.Collections;
import java.util.List;

public final class ServicePackageMapper {

    public static ServicePackageResponse toResponse(ServicePackage p) {
        List<CompanyServiceResponse> services = p.getServices() == null
            ? Collections.emptyList()
            : p.getServices().stream()
                .filter(s -> !s.isDeleted())
                .map(CompanyServiceMapper::toResponse)
                .toList();

        ServicePackageResponse r = new ServicePackageResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setNameBn(p.getNameBn());
        r.setDescription(p.getDescription());
        r.setDescriptionBn(p.getDescriptionBn());
        r.setIconUrl(p.getIconUrl());
        r.setPackagePrice(p.getPackagePrice());
        r.setDiscountPercent(p.getDiscountPercent());
        r.setBillingCycle(p.getBillingCycle());
        r.setRequestQuota(p.getRequestQuota());
        r.setAutoRenew(p.isAutoRenew());
        r.setActive(p.isActive());
        r.setDeliveryDays(p.getDeliveryDays());
        r.setTerms(p.getTerms());
        r.setFeatured(p.isFeatured());
        r.setPopular(p.isPopular());
        r.setEffectivePrice(p.getEffectivePrice());
        r.setTotalServicePrice(p.getTotalServicePrice());
        r.setServices(services);
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    public static PackageSubscriptionResponse toSubscriptionResponse(PackageSubscription s) {
        Client client = s.getClient();
        User clientUser = client != null ? client.getUser() : null;
        ServicePackage pkg = s.getServicePackage();

        PackageSubscriptionResponse r = new PackageSubscriptionResponse();
        r.setId(s.getId());
        r.setPackageId(pkg != null ? pkg.getId() : null);
        r.setPackageName(pkg != null ? pkg.getName() : null);
        r.setClientId(client != null ? client.getId() : null);
        r.setClientName(clientUser != null ? clientUser.getFullName() : null);
        r.setStatus(s.getStatus());
        r.setBillingCycle(s.getBillingCycle());
        r.setStartDate(s.getStartDate());
        r.setEndDate(s.getEndDate());
        r.setNextBillingDate(s.getNextBillingDate());
        r.setPricePaid(s.getPricePaid());
        r.setRequestQuota(s.getRequestQuota());
        r.setRequestsUsed(s.getRequestsUsed());
        r.setRemainingRequests(s.getRemainingRequests());
        r.setAutoRenew(s.isAutoRenew());
        r.setActivatedAt(s.getActivatedAt());
        r.setCancelledAt(s.getCancelledAt());
        r.setCancellationReason(s.getCancellationReason());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}
