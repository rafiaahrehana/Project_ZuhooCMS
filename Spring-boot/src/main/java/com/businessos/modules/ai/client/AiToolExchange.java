package com.businessos.modules.ai.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * One "the model called this tool, here's what it returned" pair, fed back
 * into a follow-up callWithTools() so the provider can compose a final
 * natural-language reply grounded in the real result.
 */
@Getter
@AllArgsConstructor
public class AiToolExchange {
    private final String toolName;
    private final Map<String, Object> toolArgs;
    private final String callId;
    private final String resultText;
}
