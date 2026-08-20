package com.businessos.modules.servicedesk.dynamicform;

import java.util.List;

public interface ServiceFormFieldService {
    ServiceFormFieldResponse create(Long serviceId, ServiceFormFieldRequest request);
    ServiceFormFieldResponse update(Long id, ServiceFormFieldRequest request);
    void delete(Long id);
    List<ServiceFormFieldResponse> listByService(Long serviceId);
}
