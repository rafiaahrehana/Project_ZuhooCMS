package com.businessos.modules.servicedesk.document;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
    private String label;
    private String notes;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;
}
