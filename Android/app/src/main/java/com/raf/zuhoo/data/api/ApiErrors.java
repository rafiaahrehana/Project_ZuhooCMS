package com.raf.zuhoo.data.api;

import com.google.gson.Gson;
import com.raf.zuhoo.data.model.response.ApiErrorResponse;

import java.util.Map;

import retrofit2.Response;

public final class ApiErrors {

    // SubscriptionEnforcementFilter writes this exact literal into the `error` field when the
    // tenant's trial has lapsed. It's a 403 on every non-GET request, so it needs telling apart
    // from an ordinary permission failure — the user isn't doing anything wrong, the whole
    // account is read-only until someone pays.
    private static final String SUBSCRIPTION_EXPIRED = "Subscription Expired";

    private ApiErrors() {
    }

    // The error body is a one-shot stream, so everything the caller might want is parsed in a
    // single pass and handed back together.
    public static final class ApiError {

        private final String message;
        private final boolean subscriptionExpired;

        ApiError(String message, boolean subscriptionExpired) {
            this.message = message;
            this.subscriptionExpired = subscriptionExpired;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSubscriptionExpired() {
            return subscriptionExpired;
        }
    }

    // Field-validation failures (400) carry a fieldName->message map in `data`; every other
    // error just carries `message` (see GlobalExceptionHandler on the backend).
    public static String extractMessage(Response<?> response, String fallback) {
        return describe(response, fallback).getMessage();
    }

    public static ApiError describe(Response<?> response, String fallback) {

        if (response.errorBody() == null) {
            return new ApiError(fallback, false);
        }

        try {
            ApiErrorResponse error = new Gson()
                    .fromJson(response.errorBody().charStream(), ApiErrorResponse.class);

            if (error == null) {
                return new ApiError(fallback, false);
            }

            boolean expired = response.code() == 403
                    && SUBSCRIPTION_EXPIRED.equals(error.getError());

            if (error.getData() != null && !error.getData().isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<String, String> entry : error.getData().entrySet()) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(entry.getValue());
                }
                return new ApiError(builder.toString(), expired);
            }

            return new ApiError(error.getMessage() != null ? error.getMessage() : fallback, expired);

        } catch (Exception e) {
            return new ApiError(fallback, false);
        }
    }
}
