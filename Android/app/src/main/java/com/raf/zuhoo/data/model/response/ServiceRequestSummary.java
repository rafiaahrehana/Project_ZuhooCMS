package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ServiceRequestSummary {

    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("status")
    private String status;
    @SerializedName("priority")
    private String priority;
    @SerializedName("hubServiceName")
    private String hubServiceName;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public String getHubServiceName() {
        return hubServiceName;
    }
}
