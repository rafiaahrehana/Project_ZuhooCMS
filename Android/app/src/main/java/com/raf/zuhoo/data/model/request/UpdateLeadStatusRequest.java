package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

// The backend PATCH endpoint takes the full LeadRequest DTO but applies each field only if
// present, so sending just `status` is a valid partial update (LeadServiceImpl.updateLead()).
public class UpdateLeadStatusRequest {

    @SerializedName("status")
    private final String status;

    public UpdateLeadStatusRequest(String status) {
        this.status = status;
    }
}
