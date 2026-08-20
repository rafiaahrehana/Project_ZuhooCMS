package com.businessos.modules.ai.client;

import lombok.Getter;

import java.util.Map;

/** Either the provider replied with plain text, or it wants to call one tool. */
@Getter
public class AiToolCallOrText {

    private final String text;
    private final String toolName;
    private final Map<String, Object> toolArgs;
    // Provider-native id for this specific call (Claude's tool_use.id,
    // OpenAI/Groq's tool_calls[].id). Null for Gemini/Mock, which don't need
    // one - callWithTools() must accept it back unchanged on the follow-up
    // call so each provider can stitch its own required message shape.
    private final String callId;

    private AiToolCallOrText(String text, String toolName, Map<String, Object> toolArgs, String callId) {
        this.text = text;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.callId = callId;
    }

    public static AiToolCallOrText text(String text) {
        return new AiToolCallOrText(text, null, null, null);
    }

    public static AiToolCallOrText toolCall(String toolName, Map<String, Object> args, String callId) {
        return new AiToolCallOrText(null, toolName, args, callId);
    }

    public boolean isToolCall() {
        return toolName != null;
    }
}
