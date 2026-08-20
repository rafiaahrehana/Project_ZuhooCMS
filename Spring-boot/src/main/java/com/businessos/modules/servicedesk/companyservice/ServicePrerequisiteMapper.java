package com.businessos.modules.servicedesk.companyservice;

public class ServicePrerequisiteMapper {

    public static ServicePrerequisiteResponse toResponse(ServicePrerequisite p) {
        if (p == null) return null;

        ServicePrerequisiteResponse response = new ServicePrerequisiteResponse();
        response.setId(p.getId());
        if (p.getService() != null) {
            response.setServiceId(p.getService().getId());
        }
        if (p.getPrerequisiteService() != null) {
            response.setPrerequisiteServiceId(p.getPrerequisiteService().getId());
            response.setPrerequisiteServiceName(p.getPrerequisiteService().getName());
        }
        response.setMandatory(p.isMandatory());
        response.setMessage(p.getMessage());
        return response;
    }
}
