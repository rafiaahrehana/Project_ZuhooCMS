package com.businessos.modules.company;

import com.businessos.auth.user.User;
import com.businessos.shared.address.Address;
import com.businessos.shared.address.AddressResponse;

public class CompanyMapper {
    public static CompanyResponse toResponse(Company c) {
        User owner = c.getOwner();
        CompanyResponse r = new CompanyResponse();
        r.setId(c.getId());
        r.setCompanyName(c.getCompanyName());
        r.setSubdomain(c.getSubdomain());
        r.setCompanyEmail(c.getCompanyEmail());
        r.setCompanyPhone(c.getCompanyPhone());
        r.setWebsite(c.getWebsite());
        r.setPortalAbout(c.getPortalAbout());
        r.setTaxRegistrationNumber(c.getTaxRegistrationNumber());
        r.setBankName(c.getBankName());
        r.setBankAccountName(c.getBankAccountName());
        r.setBankAccountNumber(c.getBankAccountNumber());
        r.setBankBranch(c.getBankBranch());
        r.setFiscalYearStartMonth(c.getFiscalYearStartMonth());
        r.setBaseCurrency(c.getBaseCurrency());
        r.setStatus(c.getStatus());
        r.setSubscriptionPlan(c.getSubscriptionPlan());
        r.setSubscriptionStart(c.getSubscriptionStart());
        r.setSubscriptionEnd(c.getSubscriptionEnd());
        r.setTrialExpired(c.isTrialExpired());
        r.setOwnerId(owner != null ? owner.getId() : null);
        r.setOwnerName(owner != null ? owner.getFullName() : null);
        r.setOwnerEmail(owner != null ? owner.getEmail() : null);
        r.setCreatedAt(c.getCreatedAt());

        if (c.getLocationDetail() != null) {
            Address loc = c.getLocationDetail();
            r.setLocationDetail(AddressResponse.builder()
                    .id(loc.getId())
                    .country(loc.getCountry())
                    .level1(loc.getLevel1())
                    .level2(loc.getLevel2())
                    .level3(loc.getLevel3())
                    .level4(loc.getLevel4())
                    .streetAddress(loc.getStreetAddress())
                    .postalCode(loc.getPostalCode())
                    .apartment(loc.getApartment())
                    .build());
            r.setLocation(formatAddress(loc));
        }

        return r;
    }

    public static CompanyPublicResponse toPublicResponse(Company c) {
        CompanyPublicResponse r = new CompanyPublicResponse();
        r.setId(c.getId());
        r.setCompanyName(c.getCompanyName());
        r.setSubdomain(c.getSubdomain());
        r.setPortalAbout(c.getPortalAbout());
        r.setWebsite(c.getWebsite());
        r.setCompanyPhone(c.getCompanyPhone());
        r.setCompanyEmail(c.getCompanyEmail());
        if (c.getLocationDetail() != null) {
            r.setLocation(formatAddress(c.getLocationDetail()));
        }
        return r;
    }

    public static String formatAddress(Address a) {
        if (a == null) return null;
        StringBuilder sb = new StringBuilder();
        if (a.getStreetAddress() != null && !a.getStreetAddress().isBlank()) sb.append(a.getStreetAddress());
        if (a.getLevel3() != null && !a.getLevel3().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getLevel3());
        }
        if (a.getLevel2() != null && !a.getLevel2().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getLevel2());
        }
        if (a.getLevel1() != null && !a.getLevel1().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getLevel1());
        }
        if (a.getCountry() != null && !a.getCountry().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getCountry());
        }
        return sb.toString();
    }
}
