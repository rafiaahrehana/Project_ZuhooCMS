package com.raf.zuhoo.data.repository;

import android.content.Context;
import android.util.Log;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.local.PushTokenStore;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.request.RegisterDeviceTokenRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Keeps the backend's idea of this device in step with FCM's.
public class DeviceTokenRepository {

    private static final String TAG = "DeviceTokenRepository";

    private final ApiService apiService;
    private final PushTokenStore pushTokenStore;
    private final TokenManager tokenManager;

    public DeviceTokenRepository(Context context) {
        apiService = ApiClient.getClient(context);
        pushTokenStore = new PushTokenStore(context);
        tokenManager = new TokenManager(context);
    }

    /**
     * Called after login and whenever FCM issues a new token. Safe to call repeatedly — the
     * backend upserts by token, and the local flag stops a redundant round trip on every launch.
     */
    public void registerIfNeeded() {

        String token = pushTokenStore.getToken();

        // No token yet (FCM hasn't called back), or no session to attach it to. Either way this
        // runs again later: on the next login, or when onNewToken fires.
        if (token == null || !tokenManager.isLoggedIn() || pushTokenStore.isRegistered()) {
            return;
        }

        apiService.registerDeviceToken(new RegisterDeviceTokenRequest(token))
                .enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    pushTokenStore.setRegistered(true);
                } else {
                    Log.d(TAG, "Device token registration rejected: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Left unregistered so the next launch or token refresh retries. Failing to
                // register push is not worth interrupting the user over.
                Log.d(TAG, "Device token registration failed: " + t.getMessage());
            }
        });
    }

    /**
     * Called on sign-out, before the session token is cleared — the call needs the JWT to
     * authenticate. Fire-and-forget: logout must never be blocked by this.
     */
    public void unregister() {

        String token = pushTokenStore.getToken();

        if (token == null) {
            return;
        }

        pushTokenStore.setRegistered(false);

        apiService.unregisterDeviceToken(token).enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // nothing to do either way
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "Device token unregister failed: " + t.getMessage());
            }
        });
    }
}
