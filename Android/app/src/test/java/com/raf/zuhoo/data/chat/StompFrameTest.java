package com.raf.zuhoo.data.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The STOMP framing is hand-rolled rather than a library, so a mistake here is silent: the socket
 * connects, no exception is thrown, and messages simply never arrive.
 */
public class StompFrameTest {

    // Octal escape rather than a unicode one - unicode escapes are resolved before
    // parsing and would break the char literal.
    private static final char NUL = '\0';

    @Test
    public void parsesCommandHeadersAndBody() {

        String raw = "MESSAGE\n"
                + "destination:/user/queue/service-requests/13/messages\n"
                + "content-type:application/json\n"
                + "subscription:sub-0\n"
                + "\n"
                + "{\"id\":1,\"content\":\"hello\"}" + NUL;

        StompFrame frame = StompFrame.parse(raw);

        assertEquals("MESSAGE", frame.command);
        assertEquals("/user/queue/service-requests/13/messages", frame.headers.get("destination"));
        assertEquals("application/json", frame.headers.get("content-type"));
        assertEquals("sub-0", frame.headers.get("subscription"));
        assertEquals("{\"id\":1,\"content\":\"hello\"}", frame.body);
    }

    @Test
    public void parsesFrameWithNoBody() {

        String raw = "CONNECTED\nversion:1.2\nheart-beat:0,0\nuser-name:27\n\n" + NUL;

        StompFrame frame = StompFrame.parse(raw);

        assertEquals("CONNECTED", frame.command);
        assertEquals("27", frame.headers.get("user-name"));
        assertEquals("", frame.body);
    }

    @Test
    public void toleratesMissingTrailingNul() {

        // Defensive: a frame that arrives without its terminator must not lose its last character.
        StompFrame frame = StompFrame.parse("CONNECTED\nversion:1.2\n\n");

        assertEquals("CONNECTED", frame.command);
        assertEquals("1.2", frame.headers.get("version"));
    }

    @Test
    public void keepsColonsInsideHeaderValues() {

        // Destinations and timestamps contain colons; splitting on every colon would truncate them.
        StompFrame frame = StompFrame.parse("MESSAGE\nreply-to:http://host:8086/x\n\nbody" + NUL);

        assertEquals("http://host:8086/x", frame.headers.get("reply-to"));
    }

    @Test
    public void bodyContainingBlankLinesSurvives() {

        // Only the FIRST blank line separates headers from body — a body with its own blank lines
        // must come through whole.
        StompFrame frame = StompFrame.parse("MESSAGE\nx:1\n\nline one\n\nline two" + NUL);

        assertEquals("line one\n\nline two", frame.body);
    }

    @Test
    public void encodedFramesAreNulTerminated() {

        assertTrue(StompFrame.encodeConnect().endsWith(String.valueOf(NUL)));
        assertTrue(StompFrame.encodeSubscribe("sub-1", "/user/queue/x").endsWith(String.valueOf(NUL)));
        assertTrue(StompFrame.encodeUnsubscribe("sub-1").endsWith(String.valueOf(NUL)));
        assertTrue(StompFrame.encodeDisconnect().endsWith(String.valueOf(NUL)));
    }

    @Test
    public void subscribeCarriesIdAndDestination() {

        String frame = StompFrame.encodeSubscribe("sub-7", "/user/queue/notifications");

        assertTrue(frame.startsWith("SUBSCRIBE\n"));
        assertTrue(frame.contains("\nid:sub-7\n"));
        assertTrue(frame.contains("\ndestination:/user/queue/notifications\n"));
    }

    @Test
    public void connectFrameRoundTripsThroughTheParser() {

        StompFrame frame = StompFrame.parse(StompFrame.encodeConnect());

        assertEquals("CONNECT", frame.command);
        assertEquals("1.2", frame.headers.get("accept-version"));
    }
}
