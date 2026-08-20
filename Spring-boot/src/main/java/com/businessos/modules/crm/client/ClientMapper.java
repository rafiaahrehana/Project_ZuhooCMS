package com.businessos.modules.crm.client;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;

public class ClientMapper {
    public static ClientResponse toResponse(Client client) {
        return toResponse(client, null, null);
    }

    /**
     * Won-opportunity clients (the Phase 1 CRM pipeline default) don't get a
     * portal login, so client.getUser() - the only place email/phone came
     * from - is null for most clients created that way. fallbackEmail/Phone
     * let callers pass the client's primary ClientContact instead, so the
     * detail page doesn't show blank contact info that exists right next to
     * it in the Contacts card.
     */
    public static ClientResponse toResponse(Client client, String fallbackEmail, String fallbackPhone) {
        User u = client.getUser();
        Employee employee = client.getAccountManager();
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setUserId(u != null ? u.getId():null);
        response.setFirstName(u != null ? u.getFirstName():null);
        response.setLastName(u != null ? u.getLastName():null);
        response.setEmail(u != null ? u.getEmail() : fallbackEmail);
        response.setPhone(u != null ? u.getPhone() : fallbackPhone);
        response.setImage(u != null ? u.getImage():null);
        response.setClientCompanyName(client.getClientCompanyName());
        response.setIndustry(client.getIndustry());
        response.setWebsite(client.getWebsite());
        response.setTaxId(client.getTaxId());
        response.setStatus(client.getStatus());
        response.setPortalAccessEnabled(client.isPortalAccessEnabled());
        response.setAccountManagerId(employee != null ? employee.getId() : null);
        response.setAccountManagerName(employee != null && employee.getUser() != null ? employee.getUser().getFullName() : null);
        response.setOnboardedAt(client.getOnboardedAt());
        response.setCreatedAt(client.getCreatedAt());
        response.setBillingAddress(client.getBillingAddress());
        response.setShippingAddress(client.getShippingAddress());
        response.setTags(client.getTags());
        response.setEmployeeCount(client.getEmployeeCount());
        response.setAnnualRevenue(client.getAnnualRevenue());
        response.setLifetimeValue(client.getLifetimeValue());
        response.setTotalRequests(client.getTotalRequests());
        response.setTagList(client.getTagEntities() != null
                ? com.businessos.modules.crm.tag.TagMapper.toResponseList(client.getTagEntities())
                : java.util.List.of());
        return response;
    }
}
