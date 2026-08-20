package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("type")
    private String type;
    @SerializedName("title")
    private String title;
    @SerializedName("message")
    private String message;
    @SerializedName("read")
    private boolean read;
    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
