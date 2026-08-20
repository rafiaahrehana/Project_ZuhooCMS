package com.businessos.modules.ai.util;

import org.springframework.stereotype.Component;

@Component
public class AiTokenCounter {

    private static final int CHARS_PER_TOKEN = 4;

    public int estimate(String text) {
        if (text == null || text.isBlank())
            return 0;
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}
