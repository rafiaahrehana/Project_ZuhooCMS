package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

// Only the token travels. The backend reads the email out of the verified token — sending one
// from here would be meaningless at best and a way in at worst.
public class GoogleAuthRequest {

    @SerializedName("idToken")
    private final String idToken;

    public GoogleAuthRequest(String idToken) {
        this.idToken = idToken;
    }
}
