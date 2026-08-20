package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

// One endpoint, two outcomes — see GoogleSignInResponse on the backend.
public class GoogleSignInResponse {

    @SerializedName("registered")
    private boolean registered;
    @SerializedName("login")
    private LoginResponse login;
    @SerializedName("email")
    private String email;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("lastName")
    private String lastName;

    /** true -> `login` holds the session. false -> this Google account has no user yet. */
    public boolean isRegistered() {
        return registered;
    }

    public LoginResponse getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
