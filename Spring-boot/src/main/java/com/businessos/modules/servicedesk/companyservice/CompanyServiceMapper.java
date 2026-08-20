package com.businessos.modules.servicedesk.companyservice;

import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.modules.servicedesk.servicetemplate.ServiceTemplate;

public final class CompanyServiceMapper {

    public static CompanyServiceResponse toResponse(CompanyService s) {
        ServiceCategory cat = s.getCategory();
        WorkflowTemplate wf = s.getWorkflowTemplate();
        CompanyServiceResponse r = new CompanyServiceResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setNameBn(s.getNameBn());
        r.setDescription(s.getDescription());
        r.setDescriptionBn(s.getDescriptionBn());
        r.setPrice(s.getPrice());
        r.setPriceType(s.getPriceType());
        r.setEstimatedDays(s.getEstimatedDays());
        r.setDefaultPriority(s.getDefaultPriority());
        r.setActive(s.isActive());
        r.setCategoryId(cat != null ? cat.getId() : null);
        r.setCategoryName(cat != null ? cat.getName() : null);
        r.setWorkflowTemplateId(wf != null ? wf.getId() : null);
        r.setWorkflowTemplateName(wf != null ? wf.getName() : null);
        r.setCreatedAt(s.getCreatedAt());

        ServiceTemplate st = s.getServiceTemplate();
        r.setServiceTemplateId(st != null ? st.getId() : null);
        r.setServiceTemplateName(st != null ? st.getName() : null);
        r.setCurrency(s.getCurrency());
        r.setFeatured(s.isFeatured());
        r.setRemote(s.isRemote());
        r.setOnSite(s.isOnSite());
        r.setOnline(s.isOnline());
        r.setMaximumOrders(s.getMaximumOrders());
        r.setAutoApproval(s.isAutoApproval());
        r.setRequiresQuotation(s.isRequiresQuotation());
        r.setRequiresDocuments(s.isRequiresDocuments());
        r.setSupportsCustomWorkflow(s.isSupportsCustomWorkflow());
        r.setAiAssisted(s.isAiAssisted());
        r.setVisibility(s.getVisibility());

        return r;
    }
}
