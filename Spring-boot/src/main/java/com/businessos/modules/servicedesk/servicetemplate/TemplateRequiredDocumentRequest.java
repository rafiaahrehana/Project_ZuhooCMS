package com.businessos.modules.servicedesk.servicetemplate;

import lombok.Data;

@Data
public class TemplateRequiredDocumentRequest {
    private String docName;
    private String description;
    private boolean mandatory;
    private Integer maxAgeDays;
    private String allowedFormats;
    private int sortOrder;
}
