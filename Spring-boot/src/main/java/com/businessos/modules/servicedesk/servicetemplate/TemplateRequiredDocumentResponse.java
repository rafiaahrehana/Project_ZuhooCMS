package com.businessos.modules.servicedesk.servicetemplate;

import lombok.Data;

@Data
public class TemplateRequiredDocumentResponse {
    private Long id;
    private Long serviceTemplateId;
    private String docName;
    private String description;
    private boolean mandatory;
    private Integer maxAgeDays;
    private String allowedFormats;
    private int sortOrder;
}
