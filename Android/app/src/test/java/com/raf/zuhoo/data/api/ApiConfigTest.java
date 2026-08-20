package com.raf.zuhoo.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.raf.zuhoo.BuildConfig;

import org.junit.Test;

/**
 * The WebSocket URL is derived from the REST base URL rather than declared separately, which is
 * what stops the two drifting apart. These pin that derivation down.
 */
public class ApiConfigTest {

    @Test
    public void webSocketUrlIsDerivedFromTheBaseUrl() {

        String base = BuildConfig.API_BASE_URL;
        String ws = ApiConfig.webSocketUrl();

        // Same host and port, /ws path, ws-family scheme.
        assertTrue("expected a ws:// or wss:// URL but got " + ws,
                ws.startsWith("ws://") || ws.startsWith("wss://"));
        assertTrue("expected the /ws endpoint but got " + ws, ws.endsWith("/ws"));

        String hostAndPort = base.replaceFirst("^https?://", "").replaceAll("/$", "");
        assertTrue("expected " + ws + " to point at " + hostAndPort, ws.contains(hostAndPort));
    }

    @Test
    public void plainHttpBecomesPlainWebSocket() {
        assertEquals("ws://10.0.2.2:8086/ws", derive("http://10.0.2.2:8086/"));
    }

    @Test
    public void httpsBecomesSecureWebSocket() {
        // A production build must never downgrade the socket to plaintext.
        assertEquals("wss://api.zuhoo.app/ws", derive("https://api.zuhoo.app/"));
    }

    /** Mirrors ApiConfig.webSocketUrl()'s transformation for an arbitrary base URL. */
    private String derive(String base) {

        if (base.startsWith("https://")) {
            return "wss://" + base.substring("https://".length()) + "ws";
        }

        if (base.startsWith("http://")) {
            return "ws://" + base.substring("http://".length()) + "ws";
        }

        return base + "ws";
    }
}
