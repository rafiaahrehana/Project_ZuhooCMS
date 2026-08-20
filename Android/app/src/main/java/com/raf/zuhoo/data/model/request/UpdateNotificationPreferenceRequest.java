package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class UpdateNotificationPreferenceRequest {

    @SerializedName("emailOnServiceRequest")
    private final boolean emailOnServiceRequest;
    @SerializedName("emailOnStatusChange")
    private final boolean emailOnStatusChange;
    @SerializedName("emailOnInvoice")
    private final boolean emailOnInvoice;
    @SerializedName("emailOnPayment")
    private final boolean emailOnPayment;
    @SerializedName("emailOnTaskAssigned")
    private final boolean emailOnTaskAssigned;
    @SerializedName("emailOnLeaveUpdate")
    private final boolean emailOnLeaveUpdate;
    @SerializedName("inAppOnServiceRequest")
    private final boolean inAppOnServiceRequest;
    @SerializedName("inAppOnStatusChange")
    private final boolean inAppOnStatusChange;
    @SerializedName("emailMarketing")
    private final boolean emailMarketing;

    public UpdateNotificationPreferenceRequest(boolean emailOnServiceRequest, boolean emailOnStatusChange,
                                               boolean emailOnInvoice, boolean emailOnPayment,
                                               boolean emailOnTaskAssigned, boolean emailOnLeaveUpdate,
                                               boolean inAppOnServiceRequest, boolean inAppOnStatusChange,
                                               boolean emailMarketing) {
        this.emailOnServiceRequest = emailOnServiceRequest;
        this.emailOnStatusChange = emailOnStatusChange;
        this.emailOnInvoice = emailOnInvoice;
        this.emailOnPayment = emailOnPayment;
        this.emailOnTaskAssigned = emailOnTaskAssigned;
        this.emailOnLeaveUpdate = emailOnLeaveUpdate;
        this.inAppOnServiceRequest = inAppOnServiceRequest;
        this.inAppOnStatusChange = inAppOnStatusChange;
        this.emailMarketing = emailMarketing;
    }
}
