package com.businessos.modules.crm.contact;

public class ClientContactMapper {

    public static ClientContactResponse toResponse(ClientContact contact) {
        ClientContactResponse response = new ClientContactResponse();
        response.setId(contact.getId());
        response.setClientId(contact.getClient() != null ? contact.getClient().getId() : null);
        response.setClientCompanyName(contact.getClient() != null ? contact.getClient().getClientCompanyName() : null);
        response.setFullName(contact.getFullName());
        response.setEmail(contact.getEmail());
        response.setPhone(contact.getPhone());
        response.setJobTitle(contact.getJobTitle());
        response.setDepartment(contact.getDepartment());
        response.setPrimaryContact(contact.isPrimaryContact());
        response.setNotes(contact.getNotes());
        response.setCreatedAt(contact.getCreatedAt());
        response.setUpdatedAt(contact.getUpdatedAt());
        return response;
    }
}
