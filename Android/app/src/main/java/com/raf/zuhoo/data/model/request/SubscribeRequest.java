package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class SubscribeRequest {

    @SerializedName("packageId")
    private final Long packageId;
    @SerializedName("autoRenew")
    private final Boolean autoRenew;

    public SubscribeRequest(Long packageId, Boolean autoRenew) {
        this.packageId = packageId;
        this.autoRenew = autoRenew;
    }
}
