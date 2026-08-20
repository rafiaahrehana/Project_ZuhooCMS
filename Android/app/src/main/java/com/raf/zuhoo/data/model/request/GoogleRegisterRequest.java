package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

// Second step of Google sign-up: the same token again (the backend re-verifies it) plus the
// company the user picked.
public class GoogleRegisterRequest {

    @SerializedName("idToken")
    private final String idToken;
    @SerializedName("companyId")
    private final Long companyId;
    @SerializedName("phone")
    private final String phone;
    @SerializedName("clientCompanyName")
    private final String clientCompanyName;
    @SerializedName("industry")
    private final String industry;
    @SerializedName("website")
    private final String website;

    public GoogleRegisterRequest(String idToken, Long companyId, String phone,
                                 String clientCompanyName, String industry, String website) {
        this.idToken = idToken;
        this.companyId = companyId;
        this.phone = phone;
        this.clientCompanyName = clientCompanyName;
        this.industry = industry;
        this.website = website;
    }
}
