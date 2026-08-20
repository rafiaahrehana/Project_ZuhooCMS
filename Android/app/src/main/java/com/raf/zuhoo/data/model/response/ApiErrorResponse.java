package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ApiErrorResponse {

    @SerializedName("success")
    private boolean success;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private Map<String, String> data;

    // Only SubscriptionEnforcementFilter emits this — it writes a bare {error, message} pair
    // rather than going through GlobalExceptionHandler's envelope.
    @SerializedName("error")
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getData() {
        return data;
    }
}
