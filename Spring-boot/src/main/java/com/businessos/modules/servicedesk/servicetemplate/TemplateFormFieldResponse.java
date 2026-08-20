package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.enums.FormFieldType;
import lombok.Data;

@Data
public class TemplateFormFieldResponse {
    private Long id;
    private Long serviceTemplateId;
    private String label;
    private FormFieldType fieldType;
    private boolean required;
    private String validationRules;
    private int sortOrder;
}
