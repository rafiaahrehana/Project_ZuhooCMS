package com.businessos.modules.servicedesk.servicetemplate;

import java.util.stream.Collectors;

public class ServiceTemplateMapper {

    public static ServiceTemplateResponse toResponse(ServiceTemplate template) {
        if (template == null) return null;
        
        ServiceTemplateResponse response = new ServiceTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        if (template.getCategory() != null) {
            response.setCategoryId(template.getCategory().getId());
            response.setCategoryName(template.getCategory().getName());
        }
        response.setDefaultPrice(template.getDefaultPrice());
        response.setEstimatedDays(template.getEstimatedDays());
        response.setIconUrl(template.getIconUrl());
        response.setActive(template.isActive());
        
        if (template.getFormFields() != null) {
            response.setFormFields(template.getFormFields().stream().map(f -> {
                TemplateFormFieldResponse fr = new TemplateFormFieldResponse();
                fr.setId(f.getId());
                fr.setServiceTemplateId(template.getId());
                fr.setLabel(f.getLabel());
                fr.setFieldType(f.getFieldType());
                fr.setRequired(f.isRequired());
                fr.setValidationRules(f.getValidationRules());
                fr.setSortOrder(f.getSortOrder());
                return fr;
            }).collect(Collectors.toList()));
        }
        
        if (template.getRequiredDocuments() != null) {
            response.setRequiredDocuments(template.getRequiredDocuments().stream().map(d -> {
                TemplateRequiredDocumentResponse dr = new TemplateRequiredDocumentResponse();
                dr.setId(d.getId());
                dr.setServiceTemplateId(template.getId());
                dr.setDocName(d.getDocName());
                dr.setDescription(d.getDescription());
                dr.setMandatory(d.isMandatory());
                dr.setMaxAgeDays(d.getMaxAgeDays());
                dr.setAllowedFormats(d.getAllowedFormats());
                dr.setSortOrder(d.getSortOrder());
                return dr;
            }).collect(Collectors.toList()));
        }
        
        if (template.getWorkflowStages() != null) {
            response.setWorkflowStages(template.getWorkflowStages().stream().map(s -> {
                TemplateWorkflowStageResponse sr = new TemplateWorkflowStageResponse();
                sr.setId(s.getId());
                sr.setServiceTemplateId(template.getId());
                sr.setStageName(s.getStageName());
                sr.setStageDescription(s.getStageDescription());
                sr.setStageOrder(s.getStageOrder());
                sr.setRequiresClientAction(s.isRequiresClientAction());
                sr.setRequiresPayment(s.isRequiresPayment());
                sr.setFinalStage(s.isFinalStage());
                return sr;
            }).collect(Collectors.toList()));
        }
        
        return response;
    }
}
