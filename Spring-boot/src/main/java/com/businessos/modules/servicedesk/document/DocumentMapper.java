package com.businessos.modules.servicedesk.document;

import com.businessos.auth.user.User;

public final class DocumentMapper {

    private DocumentMapper() {}

    public static DocumentResponse toResponse(Document doc) {
        DocumentResponse r = new DocumentResponse();
        r.setId(doc.getId());
        r.setFileName(doc.getFileName());
        r.setFileUrl(doc.getFileUrl());
        r.setFileType(doc.getFileType());
        r.setFileSizeBytes(doc.getFileSizeBytes());
        r.setLabel(doc.getLabel());
        r.setNotes(doc.getNotes());
        User uploadedBy = doc.getUploadedBy();
        r.setUploadedById(uploadedBy != null ? uploadedBy.getId() : null);
        r.setUploadedByName(uploadedBy != null ? uploadedBy.getFullName() : null);
        r.setCreatedAt(doc.getCreatedAt());
        return r;
    }
}
