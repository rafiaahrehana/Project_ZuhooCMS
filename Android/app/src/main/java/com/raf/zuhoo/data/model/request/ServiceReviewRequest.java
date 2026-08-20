package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ServiceReviewRequest {

    @SerializedName("serviceRequestId")
    private final Long serviceRequestId;
    @SerializedName("rating")
    private final Integer rating;
    @SerializedName("comment")
    private final String comment;

    public ServiceReviewRequest(Long serviceRequestId, Integer rating, String comment) {
        this.serviceRequestId = serviceRequestId;
        this.rating = rating;
        this.comment = comment;
    }
}
