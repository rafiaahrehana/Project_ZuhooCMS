package com.businessos.modules.ai.dto.response;

import com.businessos.modules.ai.enums.AiFeature;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiPromptTemplateResponse {

    private Long id;
    private AiFeature feature;
    private String name;
    private String template;
    private int version;
    private boolean active;
    private String changeNotes;
    private Long companyId;
    private Long updatedById;
    private String updatedByName;
    private LocalDateTime updatedAt;
}
