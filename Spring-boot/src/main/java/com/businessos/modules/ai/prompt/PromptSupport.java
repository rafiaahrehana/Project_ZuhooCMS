package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;

/**
 * Shared helpers for the *PromptBuilder classes in this package - not a base
 * class, since each builder's fields and prompt shape are otherwise unrelated
 * and inheritance would add more indirection than it saves.
 */
final class PromptSupport {

    private PromptSupport() {}

    static String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    static void requireNonBlank(String value, String fieldName, String featureName) {
        if (value == null || value.isBlank()) {
            throw new AiPromptException(fieldName + " is required for " + featureName + " prompt");
        }
    }
}
