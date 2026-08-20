package com.businessos.modules.servicedesk.dynamicform;

import com.businessos.enums.FormFieldType;
import lombok.Data;

@Data
public class ServiceFormFieldResponse {
    private Long id;
    private Long serviceId;
    private String label;
    private FormFieldType fieldType;
    private boolean required;
    private String validationRules;
    private int sortOrder;
}
