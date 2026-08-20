package com.businessos.modules.ai.enums;

public enum AiModel {

    // Google's "-latest" aliases auto-track the current available model
    // (2-week deprecation notice before the underlying version changes),
    // instead of pinning to a dated version that Google eventually retires.
    GEMINI_2_5_FLASH("gemini-flash-latest"),
    GEMINI_2_5_PRO("gemini-pro-latest"),
    GPT_4O("gpt-4o"),
    GPT_4O_MINI("gpt-4o-mini"),
    CLAUDE_SONNET("claude-sonnet-5"),
    CLAUDE_OPUS("claude-opus-5"),
    GROQ_LLAMA_3_3_70B("llama-3.3-70b-versatile"),
    GROQ_LLAMA_3_1_8B("llama-3.1-8b-instant");

    private final String modelId;

    AiModel(String modelId) {
        this.modelId = modelId;
    }

    public String getModelId() {
        return modelId;
    }
}
