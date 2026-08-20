package com.businessos.modules.ai.dto.request;

import com.businessos.modules.ai.enums.AiFeature;
import jakarta.validation.constraints.NotNull;

public class AiThreadCreateRequest {

    @NotNull(message = "Feature is required")
    private AiFeature feature;

    public AiFeature getFeature() { return feature; }
    public void setFeature(AiFeature feature) { this.feature = feature; }
}
