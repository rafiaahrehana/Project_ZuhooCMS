package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ClientResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("lastName")
    private String lastName;
    @SerializedName("email")
    private String email;
    @SerializedName("clientCompanyName")
    private String clientCompanyName;
    @SerializedName("industry")
    private String industry;
    @SerializedName("website")
    private String website;
    @SerializedName("status")
    private String status;
    @SerializedName("accountManagerName")
    private String accountManagerName;
    @SerializedName("onboardedAt")
    private String onboardedAt;
    @SerializedName("billingAddress")
    private String billingAddress;
    @SerializedName("shippingAddress")
    private String shippingAddress;

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getClientCompanyName() {
        return clientCompanyName;
    }

    public String getIndustry() {
        return industry;
    }

    public String getWebsite() {
        return website;
    }

    public String getStatus() {
        return status;
    }

    public String getAccountManagerName() {
        return accountManagerName;
    }

    public String getOnboardedAt() {
        return onboardedAt;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }
}
