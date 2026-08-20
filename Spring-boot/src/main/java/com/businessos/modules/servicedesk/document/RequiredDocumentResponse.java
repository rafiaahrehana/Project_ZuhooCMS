package com.businessos.modules.servicedesk.document;

import lombok.Data;

@Data
public class RequiredDocumentResponse {
    private Long id;
    private Long serviceId;
    private String docName;
    private String description;
    private boolean mandatory;
    private Integer maxAgeDays;
    private String allowedFormats;
    private int sortOrder;
}
