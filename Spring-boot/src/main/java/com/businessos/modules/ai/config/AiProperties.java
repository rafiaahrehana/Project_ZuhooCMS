package com.businessos.modules.ai.config;


import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Getter
@Setter
public class AiProperties {

    private AiProviderType defaultProvider = AiProviderType.GEMINI;
    private AiModel defaultModel= AiModel.GEMINI_2_5_FLASH;
    private int globalTimeoutMs = 10000;
    private int dailyCompanyLimit = 200;
    private int hourlyUserLimit   = 20;

    private final Gemini  gemini  = new Gemini();
    private final Openai  openai  = new Openai();
    private final Claude  claude  = new Claude();
    private final Groq    groq    = new Groq();

    @Getter @Setter
    public static class Gemini {
        private String apiKey;
        private double temperature = 0.7;
        private int    maxTokens   = 2048;
    }

    @Getter @Setter
    public static class Openai {
        private String apiKey;
        private double temperature = 0.7;
        private int    maxTokens   = 2048;
    }

    @Getter @Setter
    public static class Claude {
        private String apiKey;
        private double temperature = 0.7;
        private int    maxTokens   = 2048;
    }

    @Getter @Setter
    public static class Groq {
        private String apiKey;
        private double temperature = 0.7;
        private int    maxTokens   = 2048;
    }
}
