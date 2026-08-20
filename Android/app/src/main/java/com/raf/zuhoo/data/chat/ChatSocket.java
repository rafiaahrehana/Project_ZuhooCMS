package com.raf.zuhoo.data.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.raf.zuhoo.data.api.ApiConfig;
import com.raf.zuhoo.data.local.TokenManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

// One STOMP connection for the whole process (owned by AppGraph), with screens subscribing and
// unsubscribing against it — mirrors the web app's single ChatSocketService. The socket is opened
// on the first subscription and closed when the last one goes away.
//
// Receive-only by design: the backend has no @MessageMapping, so sending stays plain REST. Also
// note the server only pushes to the *other* party, never back to the sender's own sessions.
public class ChatSocket {

    public interface Listener {
        void onMessage(String destination, String jsonBody);
    }

    public interface ConnectionListener {
        void onConnectionStateChanged(boolean connected);
    }

    // Handle returned by subscribe(). Screens hold one and cancel it in onDestroy rather than
    // tearing down the shared socket.
    public final class Subscription {

        private final String destination;
        private final Listener listener;
        private boolean cancelled;

        private Subscription(String destination, Listener listener) {
            this.destination = destination;
            this.listener = listener;
        }

        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            removeSubscription(this);
        }
    }

    // Derived from the flavor's API base URL rather than declared separately, so the chat host
    // can't drift away from the REST host.
    private static final String WS_URL = ApiConfig.webSocketUrl();

    private static final long[] RECONNECT_BACKOFF_MS = {1000, 2000, 4000, 8000, 16000, 30000};

    private final TokenManager tokenManager;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> subscriptionIds = new ConcurrentHashMap<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextSubId = new AtomicInteger();

    // Connection state is mutated from the OkHttp callback thread and read from the UI thread.
    // Everything that changes it is funnelled onto the main thread (see onDisconnected) so the
    // reconnect bookkeeping stays single-threaded; volatile covers the cross-thread reads.
    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile boolean shuttingDown;
    private int reconnectAttempt;
    private Runnable pendingReconnect;

    public ChatSocket(Context context) {
        tokenManager = new TokenManager(context);
        client = new OkHttpClient.Builder()
                // A WebSocket is idle by definition between pushes; a read timeout would kill it.
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public boolean isConnected() {
        return connected;
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
        listener.onConnectionStateChanged(connected);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public Subscription subscribe(String destination, Listener listener) {

        Subscription subscription = new Subscription(destination, listener);

        subscriptions.computeIfAbsent(destination, key -> new CopyOnWriteArrayList<>())
                .add(subscription);

        shuttingDown = false;

        if (webSocket == null) {
            openSocket();
        } else if (connected) {
            sendSubscribeFrame(destination);
        }
        // If the socket exists but hasn't finished its STOMP handshake yet, the destination is
        // picked up by the bulk re-subscribe in handleFrame() when CONNECTED arrives.

        return subscription;
    }

    private void removeSubscription(Subscription subscription) {

        List<Subscription> forDestination = subscriptions.get(subscription.destination);

        if (forDestination == null) {
            return;
        }

        forDestination.remove(subscription);

        if (!forDestination.isEmpty()) {
            return;
        }

        subscriptions.remove(subscription.destination);

        String id = subscriptionIds.remove(subscription.destination);
        if (id != null && webSocket != null && connected) {
            webSocket.send(StompFrame.encodeUnsubscribe(id));
        }

        // Nothing left to listen for — don't hold a socket (and the battery cost of one) open.
        if (subscriptions.isEmpty()) {
            shutdown();
        }
    }

    // Full teardown: on logout, or when the last subscription goes away.
    public void shutdown() {

        shuttingDown = true;
        cancelPendingReconnect();

        if (webSocket != null) {
            if (connected) {
                webSocket.send(StompFrame.encodeDisconnect());
            }
            webSocket.close(1000, "client closing");
            webSocket = null;
        }

        subscriptionIds.clear();
        setConnected(false);
        reconnectAttempt = 0;
    }

    private void openSocket() {

        // Read the token fresh on every (re)connect — a silent refresh may have replaced it
        // while the socket was down.
        String token = tokenManager.getAccessToken();

        if (token == null || token.isEmpty()) {
            return;
        }

        Request request = new Request.Builder().url(WS_URL + "?token=" + token).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
                ws.send(StompFrame.encodeConnect());
            }

            @Override
            public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                handleFrame(StompFrame.parse(text));
            }

            @Override
            public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
                onDisconnected();
            }

            @Override
            public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t,
                                  @Nullable Response response) {
                onDisconnected();
            }
        });
    }

    private void handleFrame(StompFrame frame) {

        if ("CONNECTED".equals(frame.command)) {
            reconnectAttempt = 0;
            setConnected(true);
            // Re-establish every destination that was live before the drop. Without this a
            // reconnect gives a socket that's up but silent.
            for (String destination : subscriptions.keySet()) {
                sendSubscribeFrame(destination);
            }
            return;
        }

        if (!"MESSAGE".equals(frame.command)) {
            return;
        }

        String destination = frame.headers.get("destination");

        if (destination == null) {
            return;
        }

        List<Subscription> forDestination = subscriptions.get(destination);

        if (forDestination == null || forDestination.isEmpty()) {
            return;
        }

        List<Subscription> snapshot = new ArrayList<>(forDestination);

        mainHandler.post(() -> {
            for (Subscription subscription : snapshot) {
                if (!subscription.cancelled) {
                    subscription.listener.onMessage(destination, frame.body);
                }
            }
        });
    }

    // Reachable from both the UI thread (a new subscribe) and the OkHttp thread (bulk
    // re-subscribe on CONNECTED), so claim the destination atomically — otherwise the same
    // destination can be SUBSCRIBEd twice and every message arrives in duplicate.
    private void sendSubscribeFrame(String destination) {

        WebSocket socket = webSocket;

        if (socket == null) {
            return;
        }

        String id = "sub-" + nextSubId.getAndIncrement();

        if (subscriptionIds.putIfAbsent(destination, id) != null) {
            return;
        }

        socket.send(StompFrame.encodeSubscribe(id, destination));
    }

    // Called from the OkHttp callback thread; hops to main so the reconnect state below is only
    // ever touched from one thread.
    private void onDisconnected() {

        mainHandler.post(() -> {

            setConnected(false);
            // The broker forgets our subscriptions when the session dies, so drop the ids — the
            // reconnect re-sends SUBSCRIBE for every destination still in `subscriptions`.
            subscriptionIds.clear();
            webSocket = null;

            if (shuttingDown || subscriptions.isEmpty()) {
                return;
            }

            scheduleReconnect();
        });
    }

    private void scheduleReconnect() {

        cancelPendingReconnect();

        long delay = RECONNECT_BACKOFF_MS[Math.min(reconnectAttempt, RECONNECT_BACKOFF_MS.length - 1)];
        reconnectAttempt++;

        pendingReconnect = () -> {
            pendingReconnect = null;
            if (!shuttingDown && !subscriptions.isEmpty() && webSocket == null) {
                openSocket();
            }
        };

        mainHandler.postDelayed(pendingReconnect, delay);
    }

    private void cancelPendingReconnect() {
        if (pendingReconnect != null) {
            mainHandler.removeCallbacks(pendingReconnect);
            pendingReconnect = null;
        }
    }

    private void setConnected(boolean value) {

        connected = value;

        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnectionStateChanged(value);
            }
        });
    }
}
