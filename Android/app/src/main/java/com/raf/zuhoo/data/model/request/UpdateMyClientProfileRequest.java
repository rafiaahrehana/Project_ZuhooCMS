package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class UpdateMyClientProfileRequest {

    @SerializedName("clientCompanyName")
    private final String clientCompanyName;
    @SerializedName("industry")
    private final String industry;
    @SerializedName("website")
    private final String website;
    @SerializedName("billingAddress")
    private final String billingAddress;
    @SerializedName("shippingAddress")
    private final String shippingAddress;

    public UpdateMyClientProfileRequest(String clientCompanyName, String industry, String website,
                                        String billingAddress, String shippingAddress) {
        this.clientCompanyName = clientCompanyName;
        this.industry = industry;
        this.website = website;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
    }
}
