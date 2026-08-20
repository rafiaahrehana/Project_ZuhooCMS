package com.businessos.modules.servicedesk.dynamicform;

public class ServiceFormFieldMapper {

    public static ServiceFormFieldResponse toResponse(ServiceFormField field) {
        if (field == null) return null;

        ServiceFormFieldResponse response = new ServiceFormFieldResponse();
        response.setId(field.getId());
        if (field.getService() != null) {
            response.setServiceId(field.getService().getId());
        }
        response.setLabel(field.getLabel());
        response.setFieldType(field.getFieldType());
        response.setRequired(field.isRequired());
        response.setValidationRules(field.getValidationRules());
        response.setSortOrder(field.getSortOrder());
        return response;
    }
}
