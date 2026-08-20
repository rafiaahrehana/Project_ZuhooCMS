package com.raf.zuhoo.data.api;

import com.raf.zuhoo.BuildConfig;

// Single source of truth for where the app points. The base URL comes from the build flavor
// (see app/build.gradle) and the WebSocket URL is *derived* from it rather than declared
// separately, so the REST host and the chat host can never drift apart.
public final class ApiConfig {

    private ApiConfig() {
    }

    public static String baseUrl() {
        return BuildConfig.API_BASE_URL;
    }

    public static String webSocketUrl() {

        String base = BuildConfig.API_BASE_URL;

        if (base.startsWith("https://")) {
            return "wss://" + base.substring("https://".length()) + "ws";
        }

        if (base.startsWith("http://")) {
            return "ws://" + base.substring("http://".length()) + "ws";
        }

        return base + "ws";
    }
}
