package com.businessos.modules.ai.dto.request;

import com.businessos.modules.ai.enums.AiFeature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AiGenerateRequest {

    @NotNull(message = "Feature is required")
    private AiFeature feature;

    @NotBlank(message = "Prompt is required")
    private String prompt;

    // Optional - when set, generate() prepends this thread's prior messages
    // as context and saves this exchange back onto it. Omitted entirely,
    // behavior is the pre-existing stateless single-shot call.
    private Long threadId;

    public AiFeature getFeature() { return feature; }
    public void setFeature(AiFeature feature) { this.feature = feature; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }
}
