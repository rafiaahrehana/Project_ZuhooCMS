package com.businessos.modules.servicedesk.document;

import lombok.Data;

@Data
public class RequiredDocumentRequest {
    private String docName;
    private String description;
    private boolean mandatory;
    private Integer maxAgeDays;
    private String allowedFormats;
    private int sortOrder;
}
