package com.businessos.modules.ai.resolver;

import com.businessos.modules.ai.client.ClaudeClient;
import com.businessos.modules.ai.client.GeminiClient;
import com.businessos.modules.ai.client.GroqClient;
import com.businessos.modules.ai.client.MockAiClient;
import com.businessos.modules.ai.client.OpenAiClient;
import com.businessos.modules.ai.config.AiProperties;
import com.businessos.modules.ai.entity.AiProviderConfig;
import com.businessos.modules.ai.provider.*;
import com.businessos.modules.ai.repository.AiProviderConfigRepository;
import com.businessos.modules.ai.util.AiKeyDecryptor;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor

public class AiProviderResolver {

    private final AiProviderConfigRepository configRepository;
    private final AiProperties aiProperties;
    private final GeminiClient geminiClient;
    private final ClaudeClient claudeClient;
    private final OpenAiClient openAiClient;
    private final GroqClient groqClient;
    private final MockAiClient mockAiClient;
    private final AiKeyDecryptor keyDecryptor;

    public AiProviderAdapter resolve(Long companyId) {
        if (companyId != null) {
            Optional<AiProviderConfig> config =
                configRepository.findByCompanyIdAndActiveTrue(companyId);
            if (config.isPresent()) {
                return buildFromConfig(config.get());
            }
        }

        // Fallback to global platform configuration in database (companyId = null)
        Optional<AiProviderConfig> globalConfig =
            configRepository.findByCompanyIdAndActiveTrue(null);
        if (globalConfig.isPresent()) {
            return buildFromConfig(globalConfig.get());
        }

        // Fallback to application.properties defaults
        return buildFromDefaults();
    }

    public AiProviderAdapter resolveDefault() {
        return buildFromDefaults();
    }

    private AiProviderAdapter buildFromConfig(AiProviderConfig config) {
        String apiKey = resolveApiKey(config);
        double temp   = config.getTemperature() != null
            ? config.getTemperature().doubleValue() : 0.7;
        // Null would NPE on unboxing, and a small saved value starves Gemini
        // 2.x thinking models (their reasoning tokens count against the output
        // budget, so a 256-token cap truncates the answer mid-sentence).
        Integer configured = config.getMaxTokens();
        int maxTokens = (configured == null || configured < 1024) ? 2048 : configured;

        /*
         * FIX: all switch cases used config.getProvider() and config.getModel()
         * which do not exist on AiProviderConfig. Entity getters are:
         *   getProvider()  (not getProvider())
         *   getAiModel()         (not getModel())
         */
        return switch (config.getAiProviderType()) {
            case GEMINI -> {
                geminiClient.setApiKey(apiKey);
                yield new GeminiProviderAdapter(geminiClient, config.getAiModel(), temp, maxTokens);
            }
            case CLAUDE -> {
                claudeClient.setApiKey(apiKey);
                yield new ClaudeProviderAdapter(claudeClient, config.getAiModel(), temp, maxTokens);
            }
            case OPENAI -> {
                openAiClient.setApiKey(apiKey);
                yield new OpenAiProviderAdapter(openAiClient, config.getAiModel(), temp, maxTokens);
            }
            case GROQ -> {
                groqClient.setApiKey(apiKey);
                yield new GroqProviderAdapter(groqClient, config.getAiModel(), temp, maxTokens);
            }
            case MOCK -> new MockProviderAdapter(mockAiClient);
        };
    }

    private AiProviderAdapter buildFromDefaults() {
        return switch (aiProperties.getDefaultProvider()) {
            case GEMINI -> {
                geminiClient.setApiKey(aiProperties.getGemini().getApiKey());
                yield new GeminiProviderAdapter(
                    geminiClient,
                    aiProperties.getDefaultModel(),
                    aiProperties.getGemini().getTemperature(),
                    aiProperties.getGemini().getMaxTokens()
                );
            }
            case CLAUDE -> {
                claudeClient.setApiKey(aiProperties.getClaude().getApiKey());
                yield new ClaudeProviderAdapter(
                    claudeClient,
                    aiProperties.getDefaultModel(),
                    aiProperties.getClaude().getTemperature(),
                    aiProperties.getClaude().getMaxTokens()
                );
            }
            case OPENAI -> {
                openAiClient.setApiKey(aiProperties.getOpenai().getApiKey());
                yield new OpenAiProviderAdapter(
                    openAiClient,
                    aiProperties.getDefaultModel(),
                    aiProperties.getOpenai().getTemperature(),
                    aiProperties.getOpenai().getMaxTokens()
                );
            }
            case GROQ -> {
                groqClient.setApiKey(aiProperties.getGroq().getApiKey());
                yield new GroqProviderAdapter(
                    groqClient,
                    aiProperties.getDefaultModel(),
                    aiProperties.getGroq().getTemperature(),
                    aiProperties.getGroq().getMaxTokens()
                );
            }
            case MOCK -> new MockProviderAdapter(mockAiClient);
        };
    }

    private String resolveApiKey(AiProviderConfig config) {
        if (config.getApiKeyEncrypted() != null)
            return keyDecryptor.decrypt(config.getApiKeyEncrypted());

        // FIX: was config.getProvider() — corrected to getProvider()
        return switch (config.getAiProviderType()) {
            case GEMINI -> aiProperties.getGemini().getApiKey();
            case CLAUDE -> aiProperties.getClaude().getApiKey();
            case OPENAI -> aiProperties.getOpenai().getApiKey();
            case GROQ   -> aiProperties.getGroq().getApiKey();
            case MOCK   -> "mock";
        };
    }
}
