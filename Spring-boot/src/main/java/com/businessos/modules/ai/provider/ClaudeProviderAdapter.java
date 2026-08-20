package com.businessos.modules.ai.provider;

import com.businessos.modules.ai.client.AiToolCallOrText;
import com.businessos.modules.ai.client.AiToolExchange;
import com.businessos.modules.ai.client.ClaudeClient;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import com.businessos.modules.ai.tool.AiTool;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ClaudeProviderAdapter implements AiProviderAdapter {

    private final ClaudeClient client;
    private final AiModel model;
    private final double temperature;
    private final int maxTokens;

    @Override
    public String generate(String prompt) {
        return client.call(prompt, model.getModelId(), temperature, maxTokens);
    }

    @Override
    public AiToolCallOrText callWithTools(String prompt, List<AiTool> tools, List<AiToolExchange> priorExchanges) {
        return client.callWithTools(prompt, model.getModelId(), temperature, maxTokens, tools, priorExchanges);
    }

    @Override
    public AiProviderType getProviderType() {
        return AiProviderType.CLAUDE;
    }

    @Override
    public AiModel getModel() {
        return model;
    }
}
