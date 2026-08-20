package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SupportTicketResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("ticketNumber")
    private String ticketNumber;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("categoryName")
    private String categoryName;
    @SerializedName("status")
    private String status;
    @SerializedName("priority")
    private String priority;
    @SerializedName("assignedToAgentName")
    private String assignedToAgentName;
    @SerializedName("satisfactionRating")
    private Integer satisfactionRating;
    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public String getAssignedToAgentName() {
        return assignedToAgentName;
    }

    public Integer getSatisfactionRating() {
        return satisfactionRating;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
