package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class CreateSupportTicketRequest {

    @SerializedName("title")
    private final String title;
    @SerializedName("description")
    private final String description;
    @SerializedName("categoryId")
    private final Long categoryId;
    @SerializedName("priority")
    private final String priority;

    public CreateSupportTicketRequest(String title, String description, Long categoryId, String priority) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.priority = priority;
    }
}
