package com.businessos.modules.ai.support;

/**
 * A prompt plus whatever else was assembled in the same loading transaction -
 * usually the response DTO, which has to be mapped while its entity is still
 * attached. Lets a call site return both out of
 * {@link AiTransactionBoundary#load} in one go.
 *
 * @param payload value built alongside the prompt, inside the transaction
 * @param prompt  the prompt to send to the provider
 */
public record PreparedPrompt<T>(T payload, String prompt) {}
