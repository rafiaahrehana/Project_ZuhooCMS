package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class RequestComment {

    @SerializedName("id")
    private Long id;
    @SerializedName("content")
    private String content;
    @SerializedName("visibility")
    private String visibility;
    @SerializedName("authorName")
    private String authorName;
    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
