package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AnnouncementResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("body")
    private String body;
    @SerializedName("publishedAt")
    private String publishedAt;
    @SerializedName("priority")
    private int priority;
    @SerializedName("createdByName")
    private String createdByName;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public int getPriority() {
        return priority;
    }

    public String getCreatedByName() {
        return createdByName;
    }
}
