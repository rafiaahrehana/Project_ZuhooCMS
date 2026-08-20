package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ServiceReviewResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("rating")
    private int rating;
    @SerializedName("comment")
    private String comment;

    public Long getId() {
        return id;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
