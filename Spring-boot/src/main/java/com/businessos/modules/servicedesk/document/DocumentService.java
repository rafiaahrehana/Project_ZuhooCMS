package com.businessos.modules.servicedesk.document;

import java.util.List;

public interface DocumentService {
    DocumentResponse upload(Long serviceRequestId, CreateDocumentRequest request);
    List<DocumentResponse> listForRequest(Long serviceRequestId);
    void delete(Long serviceRequestId, Long documentId);
}
