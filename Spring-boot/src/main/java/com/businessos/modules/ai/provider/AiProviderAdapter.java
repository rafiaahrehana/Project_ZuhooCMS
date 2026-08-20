package com.businessos.modules.ai.provider;

import com.businessos.modules.ai.client.AiToolCallOrText;
import com.businessos.modules.ai.client.AiToolExchange;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import com.businessos.modules.ai.tool.AiTool;

import java.util.List;

/**
 * Business abstraction over one AI provider.
 * Wraps an AiHttpClient with resolved credentials and model config.
 * AiProviderResolver builds the correct adapter at runtime.
 */
public interface AiProviderAdapter {

    String generate(String prompt);

    AiToolCallOrText callWithTools(String prompt, List<AiTool> tools, List<AiToolExchange> priorExchanges);

    AiProviderType getProviderType();

    AiModel getModel();
}
