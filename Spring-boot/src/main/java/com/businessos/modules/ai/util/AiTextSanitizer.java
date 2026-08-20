package com.businessos.modules.ai.util;

import org.springframework.stereotype.Component;


/**
 * Strips characters that providers reject inside a JSON string body - control
 * characters other than tab/newline/carriage-return - and trims surrounding
 * whitespace.
 *
 * It deliberately does NOT escape backslashes or rewrite double quotes: the
 * outgoing body is serialised by Jackson via RestTemplate, which escapes both
 * correctly. Doing it here as well double-escaped every backslash and turned
 * every quote in the user's prompt into an apostrophe, corrupting code snippets
 * and quoted text before the model ever saw them.
 */
@Component
public class AiTextSanitizer {

    private static final String ILLEGAL_CONTROL_CHARS = "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]";

    public String sanitize(String input) {
        if (input == null)
            return "";
        return input
            .replaceAll(ILLEGAL_CONTROL_CHARS, "")
            .trim();
    }
}
