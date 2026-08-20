package com.businessos.modules.ai.dto.request;

import com.businessos.modules.ai.enums.AiFeature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPromptTemplateRequest {

    @NotNull(message = "Feature is required")
    private AiFeature feature;

    @NotBlank(message = "Template name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Template content is required")
    private String template;

    @Size(max = 500)
    private String changeNotes;
}
