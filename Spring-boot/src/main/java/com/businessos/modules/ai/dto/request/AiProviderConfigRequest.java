package com.businessos.modules.ai.dto.request;

import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import jakarta.validation.constraints.NotNull;

public class AiProviderConfigRequest {
    @NotNull(message = "Provider type is required")
    private AiProviderType aiProviderType;
    @NotNull(message = "Model is required")
    private AiModel model;
    private String apiKey;
    private Double temperature;
    private Integer maxTokens;

    public AiProviderType getAiProviderType() { return aiProviderType; }
    public void setAiProviderType(AiProviderType aiProviderType) { this.aiProviderType = aiProviderType; }
    public AiModel getModel() { return model; }
    public void setModel(AiModel model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}
