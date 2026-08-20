package com.raf.zuhoo.data.api;

import android.content.Context;

import com.raf.zuhoo.data.local.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        tokenManager = new TokenManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request original = chain.request();

        String token = tokenManager.getAccessToken();

        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }

        Request authorized = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authorized);
    }
}
