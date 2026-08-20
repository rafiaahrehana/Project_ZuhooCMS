package com.businessos.modules.servicedesk.document;

import java.util.List;

public interface RequiredDocumentService {
    RequiredDocumentResponse create(Long serviceId, RequiredDocumentRequest request);
    RequiredDocumentResponse update(Long id, RequiredDocumentRequest request);
    void delete(Long id);
    List<RequiredDocumentResponse> listByService(Long serviceId);
}
