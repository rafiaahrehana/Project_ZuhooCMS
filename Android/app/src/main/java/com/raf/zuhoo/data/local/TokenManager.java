package com.raf.zuhoo.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {

    private static final String PREF_NAME = "zuhoo_secure_prefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_COMPANY_ID = "company_id";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    private final SharedPreferences preferences;

    public TokenManager(Context context) {
        preferences = createEncryptedPreferences(context.getApplicationContext());
    }

    private SharedPreferences createEncryptedPreferences(Context context) {

        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create encrypted preferences", e);
        }
    }

    public void saveSession(String accessToken, String refreshToken, String role,
                            long companyId, long userId, String firstName) {

        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_ROLE, role)
                .putLong(KEY_COMPANY_ID, companyId)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_FIRST_NAME, firstName)
                .apply();
    }

    // Silent-refresh path (TokenAuthenticator): swap the token pair only. Role, companyId,
    // userId and firstName are unchanged by a refresh, so saveSession() would be wrong here —
    // it takes them as arguments and the authenticator has no business re-deriving them.
    public void updateTokens(String accessToken, String refreshToken) {

        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .commit();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    public long getCompanyId() {
        return preferences.getLong(KEY_COMPANY_ID, -1);
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1);
    }

    public String getFirstName() {
        return preferences.getString(KEY_FIRST_NAME, null);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public boolean isBiometricEnabled() {
        return preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
