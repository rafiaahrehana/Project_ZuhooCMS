package com.businessos.modules.ai.client;

import com.businessos.modules.ai.tool.AiTool;

import java.util.List;

public interface AiHttpClient {

    String call(String prompt, String model, double temperature, int maxTokens);

    /**
     * Like {@link #call}, but the model may choose to invoke one of {@code tools}
     * instead of answering directly. {@code priorExchanges} is empty on the
     * first call in a turn; when a tool was called, the agent loop executes
     * it and calls this again with that result in {@code priorExchanges} so
     * the provider can compose its final answer grounded in the real data.
     */
    AiToolCallOrText callWithTools(String prompt, String model, double temperature, int maxTokens,
                                    List<AiTool> tools, List<AiToolExchange> priorExchanges);
}
