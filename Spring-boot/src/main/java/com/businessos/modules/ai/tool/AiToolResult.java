package com.businessos.modules.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What a tool execution produced - fed back to the model (as text via
 * {@link #forModel()}) so it can compose a natural-language reply from real
 * data, rather than exposed to the frontend directly.
 */
@Getter
@AllArgsConstructor
public class AiToolResult {

    private final boolean success;
    private final String message;
    private final Object data;

    public static AiToolResult ok(String message, Object data) {
        return new AiToolResult(true, message, data);
    }

    public static AiToolResult failure(String message) {
        return new AiToolResult(false, message, null);
    }

    /** Plain-text form handed back to the model as the tool's result. */
    public String forModel() {
        return message;
    }
}
