package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class PublicClientRegisterRequest {

    @SerializedName("firstName")
    private final String firstName;
    @SerializedName("lastName")
    private final String lastName;
    @SerializedName("email")
    private final String email;
    @SerializedName("password")
    private final String password;
    @SerializedName("phone")
    private final String phone;
    @SerializedName("companyId")
    private final Long companyId;
    @SerializedName("clientCompanyName")
    private final String clientCompanyName;
    @SerializedName("industry")
    private final String industry;
    @SerializedName("website")
    private final String website;

    public PublicClientRegisterRequest(String firstName, String lastName, String email,
                                       String password, String phone, Long companyId,
                                       String clientCompanyName, String industry, String website) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.companyId = companyId;
        this.clientCompanyName = clientCompanyName;
        this.industry = industry;
        this.website = website;
    }
}
