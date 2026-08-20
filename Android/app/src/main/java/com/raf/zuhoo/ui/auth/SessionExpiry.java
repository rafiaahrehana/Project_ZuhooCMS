package com.raf.zuhoo.ui.auth;

import android.content.Context;
import android.content.Intent;

import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.local.TokenManager;

import java.util.concurrent.atomic.AtomicBoolean;

// Terminal end of the auth path: the refresh token itself was rejected, so there is nothing left
// to retry with. Clears the session and sends the user back to the login screen.
//
// Called from TokenAuthenticator on an OkHttp background thread, and several in-flight requests
// can fail at once — the AtomicBoolean makes sure a burst of simultaneous 401s produces one trip
// to the login screen rather than a stack of them.
public final class SessionExpiry {

    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    private static final AtomicBoolean routing = new AtomicBoolean(false);

    private SessionExpiry() {
    }

    public static void onSessionExpired(Context context) {

        if (!routing.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();

        // The socket authenticates with the same token, so it's dead too — close it rather than
        // leaving it to fail and retry against a session that no longer exists.
        ZuhooApplication.graph().notificationCenter().stop();
        ZuhooApplication.graph().chatSocket().shutdown();
        // Same reasoning as logout: the cached lists belong to the session that just ended.
        ZuhooApplication.graph().listCache().wipe();

        new TokenManager(appContext).clearSession();

        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.putExtra(EXTRA_SESSION_EXPIRED, true);
        // NEW_TASK is required because this is launched from an application context, off the
        // activity stack; CLEAR_TASK drops whatever screen the user was on so they can't back
        // into a signed-out view.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }

    // LoginActivity re-arms this once it's on screen, so a later session can expire too.
    static void reset() {
        routing.set(false);
    }
}
