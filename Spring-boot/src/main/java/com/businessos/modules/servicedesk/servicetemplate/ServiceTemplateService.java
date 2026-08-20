package com.businessos.modules.servicedesk.servicetemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ServiceTemplateService {
    ServiceTemplateResponse create(ServiceTemplateRequest request);
    ServiceTemplateResponse update(Long id, ServiceTemplateRequest request);
    ServiceTemplateResponse getById(Long id);
    Page<ServiceTemplateResponse> listAll(boolean activeOnly, Pageable pageable);
    List<ServiceTemplateResponse> listByCategory(Long categoryId);
    void delete(Long id);
}
