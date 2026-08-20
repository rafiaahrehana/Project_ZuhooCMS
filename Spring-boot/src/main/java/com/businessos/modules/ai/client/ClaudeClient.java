package com.businessos.modules.ai.client;


import com.businessos.modules.ai.exception.AiProviderException;
import com.businessos.modules.ai.tool.AiTool;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor

public class ClaudeClient implements AiHttpClient {

    private static final String BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Qualifier("aiRestTemplate")
    private final RestTemplate aiRestTemplate;

    @Setter
    private String apiKey;

    @Override
    public String call(String prompt, String model, double temperature, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        Map<String, Object> body = Map.of(
            "model",      model,
            "max_tokens", maxTokens,
            "temperature", temperature,
            "messages",   List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return extractText(response.getBody());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Claude API call failed: " + e.getMessage(), isRetryable(e));
        }
    }

    private String extractText(Map<?, ?> response) {
        try {
            List<?> content = (List<?>) response.get("content");
            return (String) ((Map<?, ?>) content.get(0)).get("text");
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse Claude response: " + e.getMessage());
        }
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof HttpClientErrorException client) {
            return client.getStatusCode().value() == 429;
        }
        return true;
    }

    @Override
    public AiToolCallOrText callWithTools(String prompt, String model, double temperature, int maxTokens,
                                           List<AiTool> tools, List<AiToolExchange> priorExchanges) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        List<Object> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        for (AiToolExchange ex : priorExchanges) {
            messages.add(Map.of("role", "assistant", "content", List.of(Map.of(
                "type", "tool_use", "id", ex.getCallId(), "name", ex.getToolName(), "input", ex.getToolArgs()
            ))));
            messages.add(Map.of("role", "user", "content", List.of(Map.of(
                "type", "tool_result", "tool_use_id", ex.getCallId(), "content", ex.getResultText()
            ))));
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("messages", messages);
        // Omitted once a tool result is already in play, forcing a text-only
        // reply - the v1 agent loop handles at most one tool call per turn.
        if (priorExchanges.isEmpty()) {
            body.put("tools", tools.stream().map(t -> (Object) Map.of(
                "name", t.name(), "description", t.description(), "input_schema", t.parametersSchema()
            )).toList());
        }

        try {
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return extractToolCallOrText(response.getBody());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Claude tool-call failed: " + e.getMessage(), isRetryable(e));
        }
    }

    @SuppressWarnings("unchecked")
    private AiToolCallOrText extractToolCallOrText(Map<?, ?> response) {
        try {
            List<?> content = (List<?>) response.get("content");
            StringBuilder text = new StringBuilder();
            for (Object block : content) {
                Map<?, ?> b = (Map<?, ?>) block;
                if ("tool_use".equals(b.get("type"))) {
                    Map<String, Object> input = b.get("input") != null
                        ? (Map<String, Object>) b.get("input") : Map.of();
                    return AiToolCallOrText.toolCall((String) b.get("name"), input, (String) b.get("id"));
                }
                if ("text".equals(b.get("type"))) {
                    text.append(b.get("text"));
                }
            }
            return AiToolCallOrText.text(text.toString());
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse Claude tool-call response: " + e.getMessage());
        }
    }
}
