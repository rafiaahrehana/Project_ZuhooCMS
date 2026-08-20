package com.raf.zuhoo.data.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.request.RefreshTokenRequest;
import com.raf.zuhoo.data.model.response.JwtResponse;
import com.raf.zuhoo.ui.auth.SessionExpiry;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

// Silent re-auth. The access token lives 15 minutes (jwt.access-expiration-ms=900000) while the
// refresh token lives 7 days, so without this every session dies quietly a quarter of an hour in.
//
// OkHttp calls authenticate() only on a 401. That matters: the backend returns 401 for an
// expired/invalid token and 403 for a genuine permission failure
// (SecurityConfig -> HttpStatusEntryPoint(UNAUTHORIZED)), so a 403 must never be retried — and
// with this contract it never reaches here in the first place.
public class TokenAuthenticator implements Authenticator {

    // Unauthenticated endpoints. A 401 from these is a real answer ("wrong password"), not an
    // expired token — refreshing would be nonsense, and on the login screen it would also wipe
    // the session of a user who simply typo'd their password.
    private static final String[] NO_REFRESH_PATHS = {
            "api/auth/login",
            "api/auth/refresh",
            "api/auth/register",
            "api/auth/forgot-password",
            "api/auth/reset-password",
            "api/auth/verify-email",
            "api/auth/resend-verification",
            "api/clients/public/register",
    };

    private static final Object REFRESH_LOCK = new Object();

    private final Context appContext;
    private final TokenManager tokenManager;

    public TokenAuthenticator(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {

        if (isUnauthenticatedPath(response.request().url().encodedPath())) {
            return null;
        }

        // We already retried this request once with a fresh token and still got a 401. Retrying
        // again would loop; give up and let the caller surface the failure.
        if (priorResponseCount(response) >= 1) {
            return null;
        }

        String failedToken = bearerOf(response.request());

        synchronized (REFRESH_LOCK) {

            // Another thread may have refreshed while this one was queued on the lock. If the
            // stored token has moved on, just retry with it instead of burning a second refresh.
            String current = tokenManager.getAccessToken();

            if (current != null && !current.isEmpty() && !current.equals(failedToken)) {
                return retryWith(response.request(), current);
            }

            String refreshToken = tokenManager.getRefreshToken();

            if (refreshToken == null || refreshToken.isEmpty()) {
                SessionExpiry.onSessionExpired(appContext);
                return null;
            }

            JwtResponse refreshed = requestNewTokens(refreshToken);

            if (refreshed == null || refreshed.getAccessToken() == null) {
                // Refresh token revoked or expired (7 days) — this is a real logout, not a blip.
                SessionExpiry.onSessionExpired(appContext);
                return null;
            }

            tokenManager.updateTokens(refreshed.getAccessToken(), refreshed.getRefreshToken());

            return retryWith(response.request(), refreshed.getAccessToken());
        }
    }

    // Throws IOException on a network failure rather than returning null. That distinction is the
    // whole point: a rejected refresh token means "really logged out", but an unreachable server
    // means "try again later" — letting the IOException propagate fails just this one call and
    // leaves the session intact instead of kicking the user to the login screen over a blip.
    private JwtResponse requestNewTokens(String refreshToken) throws IOException {

        retrofit2.Response<JwtResponse> result = TokenRefreshClient.get()
                .refresh(new RefreshTokenRequest(refreshToken))
                .execute();

        return result.isSuccessful() ? result.body() : null;
    }

    /**
     * Whether a 401 from this path means "wrong credentials" rather than "expired token".
     *
     * Package-private and static so the rule can be unit-tested without an Android context —
     * getting it wrong on the login path would wipe the session of anyone who mistypes their
     * password, which is exactly the sort of bug that only shows up in front of a user.
     */
    static boolean isUnauthenticatedPath(String encodedPath) {

        if (encodedPath == null) {
            return false;
        }

        for (String excluded : NO_REFRESH_PATHS) {
            if (encodedPath.endsWith(excluded) || encodedPath.endsWith("/" + excluded)) {
                return true;
            }
        }

        return false;
    }

    private Request retryWith(Request original, String accessToken) {
        return original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
    }

    private String bearerOf(Request request) {
        String header = request.header("Authorization");
        return header != null && header.startsWith("Bearer ")
                ? header.substring("Bearer ".length())
                : null;
    }

    private int priorResponseCount(Response response) {
        int count = 0;
        for (Response prior = response.priorResponse(); prior != null; prior = prior.priorResponse()) {
            count++;
        }
        return count;
    }
}
