package com.raf.zuhoo.data.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// A deliberately bare Retrofit instance for the refresh call only: no AuthInterceptor (the
// endpoint is permitAll and a stale bearer header is pointless) and, critically, no
// TokenAuthenticator — otherwise a 401 from the refresh endpoint itself would re-enter the
// authenticator and recurse.
final class TokenRefreshClient {

    private static volatile ApiService instance;

    private TokenRefreshClient() {
    }

    static ApiService get() {

        ApiService local = instance;

        if (local == null) {
            synchronized (TokenRefreshClient.class) {
                local = instance;
                if (local == null) {
                    local = new Retrofit.Builder()
                            .baseUrl(ApiConfig.baseUrl())
                            .client(new OkHttpClient.Builder().build())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(ApiService.class);
                    instance = local;
                }
            }
        }

        return local;
    }
}
