package com.businessos.modules.ai.client;


import com.businessos.modules.ai.tool.AiTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockAiClient implements AiHttpClient {

    @Override
    public String call(String prompt, String model, double temperature, int maxTokens) {

        return "[MOCK AI RESPONSE] Prompt received ("
            + prompt.length() + " chars). "
            + "Configure ai.default-provider=gemini to use a real provider.";
    }

    /**
     * No real model behind this - simulates tool selection by a plain keyword
     * match against each tool's name/description, so the agent loop is
     * exercisable in demo/dev without a real provider key. Never calls a
     * tool twice in the same turn: if priorExchanges already has an entry,
     * this always answers with text instead of proposing another call.
     */
    @Override
    public AiToolCallOrText callWithTools(String prompt, String model, double temperature, int maxTokens,
                                           List<AiTool> tools, List<AiToolExchange> priorExchanges) {
        if (!priorExchanges.isEmpty()) {
            AiToolExchange last = priorExchanges.get(priorExchanges.size() - 1);
            return AiToolCallOrText.text("[MOCK] " + last.getResultText());
        }

        String lower = prompt.toLowerCase();
        for (AiTool tool : tools) {
            String haystack = (tool.name() + " " + tool.description()).toLowerCase();
            for (String word : lower.split("\\W+")) {
                if (word.length() > 3 && haystack.contains(word)) {
                    return AiToolCallOrText.toolCall(tool.name(), Map.of(), "mock-call-1");
                }
            }
        }
        return AiToolCallOrText.text(call(prompt, model, temperature, maxTokens));
    }
}
