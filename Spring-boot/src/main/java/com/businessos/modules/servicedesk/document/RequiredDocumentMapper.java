package com.businessos.modules.servicedesk.document;

public class RequiredDocumentMapper {

    public static RequiredDocumentResponse toResponse(RequiredDocument doc) {
        if (doc == null) return null;

        RequiredDocumentResponse response = new RequiredDocumentResponse();
        response.setId(doc.getId());
        if (doc.getService() != null) {
            response.setServiceId(doc.getService().getId());
        }
        response.setDocName(doc.getDocName());
        response.setDescription(doc.getDescription());
        response.setMandatory(doc.isMandatory());
        response.setMaxAgeDays(doc.getMaxAgeDays());
        response.setAllowedFormats(doc.getAllowedFormats());
        response.setSortOrder(doc.getSortOrder());
        return response;
    }
}
