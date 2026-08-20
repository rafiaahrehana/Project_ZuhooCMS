package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SupportMessageResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("ticketId")
    private Long ticketId;
    @SerializedName("sentByName")
    private String sentByName;
    @SerializedName("message")
    private String message;
    @SerializedName("internal")
    private boolean internal;
    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getSentByName() {
        return sentByName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
