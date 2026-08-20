package com.businessos.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AiAgentTurnRequest {

    @NotNull(message = "threadId is required")
    private Long threadId;

    @NotBlank(message = "message is required")
    private String message;

    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
