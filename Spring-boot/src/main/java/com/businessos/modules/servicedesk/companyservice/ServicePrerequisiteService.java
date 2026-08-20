package com.businessos.modules.servicedesk.companyservice;

import java.util.List;

public interface ServicePrerequisiteService {
    ServicePrerequisiteResponse create(Long serviceId, ServicePrerequisiteRequest request);
    void delete(Long serviceId, Long id);
    List<ServicePrerequisiteResponse> listByService(Long serviceId);
}
