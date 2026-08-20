package com.raf.zuhoo.data.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raf.zuhoo.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Host comes from the build flavor (see ApiConfig / app/build.gradle), not from a constant
    // here — a hardcoded emulator address can't ship.
    private static volatile Retrofit retrofit;

    public static ApiService getClient(Context context) {

        Retrofit local = retrofit;

        if (local == null) {
            synchronized (ApiClient.class) {
                local = retrofit;
                if (local == null) {
                    local = build(context.getApplicationContext());
                    retrofit = local;
                }
            }
        }

        return local.create(ApiService.class);
    }

    private static Retrofit build(Context appContext) {

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(appContext))
                // Runs only on a 401, i.e. an expired access token. Permission failures come back
                // as 403 and are left alone.
                .authenticator(new TokenAuthenticator(appContext));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(logging);
        }

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        return new Retrofit.Builder()
                .baseUrl(ApiConfig.baseUrl())
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
