package com.businessos.modules.ai.dto.response;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;

public class AiGenerateResponse {
    private String conversationUuid;
    private AiFeature feature;
    private AiProviderType provider;
    private AiModel model;
    private String result;
    private long executionTimeMs;
    private Long threadId;
    // True when `result` is a proposed write-action awaiting the employee's
    // next message to confirm or cancel it - the frontend renders this turn
    // as a distinct confirm/cancel card instead of a plain chat bubble.
    private boolean awaitingConfirmation;

    public String getConversationUuid() { return conversationUuid; }
    public void setConversationUuid(String conversationUuid) { this.conversationUuid = conversationUuid; }
    public AiFeature getFeature() { return feature; }
    public void setFeature(AiFeature feature) { this.feature = feature; }
    public AiProviderType getProvider() { return provider; }
    public void setProvider(AiProviderType provider) { this.provider = provider; }
    public AiModel getModel() { return model; }
    public void setModel(AiModel model) { this.model = model; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }
    public boolean isAwaitingConfirmation() { return awaitingConfirmation; }
    public void setAwaitingConfirmation(boolean awaitingConfirmation) { this.awaitingConfirmation = awaitingConfirmation; }
}
