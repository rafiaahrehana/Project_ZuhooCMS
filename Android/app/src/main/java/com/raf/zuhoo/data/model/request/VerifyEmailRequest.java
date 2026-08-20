package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class VerifyEmailRequest {

    @SerializedName("token")
    private final String token;

    public VerifyEmailRequest(String token) {
        this.token = token;
    }
}
