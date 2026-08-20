package com.businessos.modules.ai.client;

import com.businessos.modules.ai.exception.AiProviderException;
import com.businessos.modules.ai.tool.AiTool;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor

public class GeminiClient implements AiHttpClient {

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Qualifier("aiRestTemplate")
    private final RestTemplate aiRestTemplate;

    @Setter
    private String apiKey;

    @Override
    public String call(String prompt, String model, double temperature, int maxTokens) {
        // Gemini 2.5 "thinking" models spend reasoning tokens from the SAME
        // maxOutputTokens budget - left on (the default), thinking regularly
        // eats 1-2k tokens and the visible answer gets cut off mid-sentence.
        // These are single-shot business prompts, not math puzzles: turn
        // thinking off. Models that reject thinkingConfig (1.5 family, 2.5
        // Pro which can't disable it) get one retry without it.
        try {
            return doCall(prompt, model, temperature, maxTokens, true);
        } catch (HttpClientErrorException e) {
            // Google's rejection is a generic "Request contains an invalid
            // argument." with no field name, so ANY 400 on a thinking-disabled
            // request gets one retry without thinkingConfig before failing.
            if (e.getStatusCode().value() == 400) {
                try {
                    // Thinking stays on for this retry, so its reasoning tokens
                    // share the budget - give the answer room to survive them.
                    return doCall(prompt, model, temperature, Math.max(maxTokens, 8192), false);
                } catch (HttpClientErrorException retryFailure) {
                    throw new AiProviderException("Gemini API call failed: " + retryFailure.getMessage(), isRetryable(retryFailure));
                }
            }
            throw new AiProviderException("Gemini API call failed: " + e.getMessage(), isRetryable(e));
        }
    }

    private String doCall(String prompt, String model, double temperature, int maxTokens, boolean disableThinking) {
        String url = String.format(BASE_URL, model, apiKey);

        Map<String, Object> generationConfig = new java.util.HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        if (disableThinking) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        }

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", generationConfig
        );

        try {
            Map<?, ?> response = aiRestTemplate.postForObject(url, body, Map.class);
            return extractText(response);
        } catch (AiProviderException | HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Gemini API call failed: " + e.getMessage(), isRetryable(e));
        }
    }


    /**
     * Gemini 2.x models split a single answer across MULTIPLE parts (and
     * thinking models may prepend thought parts flagged "thought": true).
     * Taking parts[0] alone silently truncated every response longer than one
     * part - concatenate all non-thought text parts instead.
     */
    private String extractText(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> first    = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content  = (Map<?, ?>) first.get("content");
            List<?> parts      = (List<?>) content.get("parts");
            StringBuilder text = new StringBuilder();
            for (Object part : parts) {
                Map<?, ?> p = (Map<?, ?>) part;
                if (Boolean.TRUE.equals(p.get("thought"))) continue;
                Object t = p.get("text");
                if (t != null) text.append(t);
            }
            if (text.isEmpty()) {
                Object finishReason = first.get("finishReason");
                throw new AiProviderException("Gemini returned no text"
                        + (finishReason != null ? " (finishReason: " + finishReason + ")" : ""));
            }
            return text.toString();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    // Retry on timeouts/connection errors/5xx/429 - not on other 4xx (bad request, auth failure, etc).
    private boolean isRetryable(Exception e) {
        if (e instanceof HttpClientErrorException client) {
            return client.getStatusCode().value() == 429;
        }
        return true;
    }

    @Override
    public AiToolCallOrText callWithTools(String prompt, String model, double temperature, int maxTokens,
                                           List<AiTool> tools, List<AiToolExchange> priorExchanges) {
        String url = String.format(BASE_URL, model, apiKey);

        List<Object> contents = new ArrayList<>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", prompt))));
        for (AiToolExchange ex : priorExchanges) {
            contents.add(Map.of("role", "model", "parts",
                List.of(Map.of("functionCall", Map.of("name", ex.getToolName(), "args", ex.getToolArgs())))));
            contents.add(Map.of("role", "user", "parts",
                List.of(Map.of("functionResponse", Map.of("name", ex.getToolName(),
                    "response", Map.of("content", ex.getResultText()))))));
        }

        List<Map<String, Object>> declarations = tools.stream()
            .map(t -> {
                Map<String, Object> d = new java.util.HashMap<>();
                d.put("name", t.name());
                d.put("description", t.description());
                d.put("parameters", t.parametersSchema());
                return d;
            })
            .toList();

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("contents", contents);
        // Once a tool has already been called this turn, ask for a final
        // text answer rather than risking a second tool call the v1 loop
        // doesn't support chaining.
        if (priorExchanges.isEmpty()) {
            body.put("tools", List.of(Map.of("functionDeclarations", declarations)));
        }
        body.put("generationConfig", Map.of("temperature", temperature, "maxOutputTokens", maxTokens));

        try {
            Map<?, ?> response = aiRestTemplate.postForObject(url, body, Map.class);
            return extractToolCallOrText(response);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Gemini tool-call failed: " + e.getMessage(), isRetryable(e));
        }
    }

    @SuppressWarnings("unchecked")
    private AiToolCallOrText extractToolCallOrText(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> first    = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content  = (Map<?, ?>) first.get("content");
            List<?> parts      = (List<?>) content.get("parts");

            StringBuilder text = new StringBuilder();
            for (Object part : parts) {
                Map<?, ?> p = (Map<?, ?>) part;
                if (p.get("functionCall") != null) {
                    Map<String, Object> fc = (Map<String, Object>) p.get("functionCall");
                    Map<String, Object> args = fc.get("args") != null
                        ? (Map<String, Object>) fc.get("args") : Map.of();
                    return AiToolCallOrText.toolCall((String) fc.get("name"), args, null);
                }
                if (Boolean.TRUE.equals(p.get("thought"))) continue;
                Object t = p.get("text");
                if (t != null) text.append(t);
            }
            return AiToolCallOrText.text(text.toString());
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse Gemini tool-call response: " + e.getMessage());
        }
    }
}
