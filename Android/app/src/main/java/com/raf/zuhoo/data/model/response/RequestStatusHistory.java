package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

// Mirrors RequestStatusHistoryResponse. Returned oldest-first
// (findByServiceRequestIdOrderByChangedAtAsc), which is the order a timeline reads in.
public class RequestStatusHistory {

    @SerializedName("id")
    private Long id;
    @SerializedName("oldStatus")
    private String oldStatus;
    @SerializedName("newStatus")
    private String newStatus;
    @SerializedName("reason")
    private String reason;
    @SerializedName("changedByName")
    private String changedByName;
    @SerializedName("changedAt")
    private String changedAt;

    public Long getId() {
        return id;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public String getChangedAt() {
        return changedAt;
    }
}
