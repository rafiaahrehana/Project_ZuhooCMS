package com.businessos.modules.ai.client;

import com.businessos.modules.ai.exception.AiProviderException;
import com.businessos.modules.ai.tool.AiTool;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class OpenAiClient implements AiHttpClient {

    private static final String BASE_URL = "https://api.openai.com/v1/chat/completions";

    @Qualifier("aiRestTemplate")
    private final RestTemplate aiRestTemplate;

    @Setter
    private String apiKey;

    @Override
    public String call(String prompt, String model, double temperature, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model",       model,
            "temperature", temperature,
            "max_tokens",  maxTokens,
            "messages",    List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return extractText(response.getBody());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("OpenAI API call failed: " + e.getMessage(), isRetryable(e));
        }
    }


    private String extractText(Map<?, ?> response) {
        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse OpenAI response: " + e.getMessage());
        }
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof HttpClientErrorException client) {
            return client.getStatusCode().value() == 429;
        }
        return true;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AiToolCallOrText callWithTools(String prompt, String model, double temperature, int maxTokens,
                                           List<AiTool> tools, List<AiToolExchange> priorExchanges) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Object> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        for (AiToolExchange ex : priorExchanges) {
            Map<String, Object> assistantMsg = new java.util.HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", (Object) null);
            assistantMsg.put("tool_calls", List.of(Map.of(
                "id", ex.getCallId(),
                "type", "function",
                "function", Map.of("name", ex.getToolName(), "arguments", writeArgs(ex.getToolArgs()))
            )));
            messages.add(assistantMsg);
            messages.add(Map.of("role", "tool", "tool_call_id", ex.getCallId(), "content", ex.getResultText()));
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("messages", messages);
        if (priorExchanges.isEmpty()) {
            body.put("tools", tools.stream().map(t -> (Object) Map.of(
                "type", "function",
                "function", Map.of("name", t.name(), "description", t.description(),
                    "parameters", t.parametersSchema())
            )).toList());
        }

        try {
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return extractToolCallOrText(response.getBody());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("OpenAI tool-call failed: " + e.getMessage(), isRetryable(e));
        }
    }

    private String writeArgs(Map<String, Object> args) {
        try {
            return MAPPER.writeValueAsString(args);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private AiToolCallOrText extractToolCallOrText(Map<?, ?> response) {
        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            List<?> toolCalls = (List<?>) message.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                Map<?, ?> call = (Map<?, ?>) toolCalls.get(0);
                Map<?, ?> function = (Map<?, ?>) call.get("function");
                String argsJson = (String) function.get("arguments");
                Map<String, Object> args = argsJson == null || argsJson.isBlank()
                    ? Map.of() : MAPPER.readValue(argsJson, Map.class);
                return AiToolCallOrText.toolCall((String) function.get("name"), args, (String) call.get("id"));
            }
            return AiToolCallOrText.text((String) message.get("content"));
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse OpenAI tool-call response: " + e.getMessage());
        }
    }
}
