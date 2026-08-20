package com.raf.zuhoo.di;

import android.content.Context;

import com.raf.zuhoo.data.chat.ChatSocket;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.local.db.ZuhooDatabase;
import com.raf.zuhoo.data.notification.NotificationCenter;

// Hand-rolled DI. The app is small enough that an annotation processor would cost more than it
// saves (the planning doc says as much), and repositories are cheap wrappers that can still be
// constructed per-screen — what genuinely must be shared lives here.
public class AppGraph {

    private final Context appContext;
    private final TokenManager tokenManager;
    private final ChatSocket chatSocket;
    private final ListCache listCache;
    private final NotificationCenter notificationCenter;

    public AppGraph(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
        // One socket for the process, not one per screen — screens subscribe and unsubscribe
        // against it. Also what lets a background notification subscription stay alive while the
        // user moves between screens.
        chatSocket = new ChatSocket(appContext);
        listCache = new ListCache(ZuhooDatabase.create(appContext));
        notificationCenter = new NotificationCenter(appContext, chatSocket, tokenManager);
    }

    public ListCache listCache() {
        return listCache;
    }

    public NotificationCenter notificationCenter() {
        return notificationCenter;
    }

    public Context appContext() {
        return appContext;
    }

    public TokenManager tokenManager() {
        return tokenManager;
    }

    public ChatSocket chatSocket() {
        return chatSocket;
    }
}
