package com.businessos.modules.servicedesk.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    @Pattern(regexp = "^https?://.*$", message = "File URL must start with http:// or https://")
    private String fileUrl;

    private String fileType;
    private Long fileSizeBytes;
    private String label;
    private String notes;
}
