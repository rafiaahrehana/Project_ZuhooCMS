package com.raf.zuhoo.data.local;

import android.content.Context;
import android.content.SharedPreferences;

// Remembers the FCM token separately from the session.
//
// It has to outlive clearSession(): logging out needs the token in order to *unregister* it, and
// the token belongs to the app install rather than to any one account. It is not a credential —
// it identifies a device as a push target, so plain prefs are appropriate.
public class PushTokenStore {

    private static final String PREF_NAME = "zuhoo_push";
    private static final String KEY_TOKEN = "fcm_token";
    private static final String KEY_REGISTERED = "fcm_registered";

    private final SharedPreferences preferences;

    public PushTokenStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public void setToken(String token) {
        // A new token has not been sent to this backend yet, whatever the old flag said.
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_REGISTERED, false)
                .apply();
    }

    public boolean isRegistered() {
        return preferences.getBoolean(KEY_REGISTERED, false);
    }

    public void setRegistered(boolean registered) {
        preferences.edit().putBoolean(KEY_REGISTERED, registered).apply();
    }
}
