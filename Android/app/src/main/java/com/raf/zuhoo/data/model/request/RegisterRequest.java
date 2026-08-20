package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("firstName")
    private final String firstName;
    @SerializedName("lastName")
    private final String lastName;
    @SerializedName("email")
    private final String email;
    @SerializedName("password")
    private final String password;
    @SerializedName("companyName")
    private final String companyName;
    @SerializedName("subdomain")
    private final String subdomain;
    @SerializedName("companyEmail")
    private final String companyEmail;
    @SerializedName("companyPhone")
    private final String companyPhone;

    public RegisterRequest(String firstName, String lastName, String email, String password,
                           String companyName, String subdomain,
                           String companyEmail, String companyPhone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.companyName = companyName;
        this.subdomain = subdomain;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
    }
}
