package com.businessos.modules.finance.reconciliation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttachStatementRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;
}
