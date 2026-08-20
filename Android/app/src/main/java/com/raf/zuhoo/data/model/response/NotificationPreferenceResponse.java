package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class NotificationPreferenceResponse {

    @SerializedName("emailOnServiceRequest")
    private boolean emailOnServiceRequest;
    @SerializedName("emailOnStatusChange")
    private boolean emailOnStatusChange;
    @SerializedName("emailOnInvoice")
    private boolean emailOnInvoice;
    @SerializedName("emailOnPayment")
    private boolean emailOnPayment;
    @SerializedName("emailOnTaskAssigned")
    private boolean emailOnTaskAssigned;
    @SerializedName("emailOnLeaveUpdate")
    private boolean emailOnLeaveUpdate;
    @SerializedName("inAppOnServiceRequest")
    private boolean inAppOnServiceRequest;
    @SerializedName("inAppOnStatusChange")
    private boolean inAppOnStatusChange;
    @SerializedName("emailMarketing")
    private boolean emailMarketing;

    public boolean isEmailOnServiceRequest() {
        return emailOnServiceRequest;
    }

    public boolean isEmailOnStatusChange() {
        return emailOnStatusChange;
    }

    public boolean isEmailOnInvoice() {
        return emailOnInvoice;
    }

    public boolean isEmailOnPayment() {
        return emailOnPayment;
    }

    public boolean isEmailOnTaskAssigned() {
        return emailOnTaskAssigned;
    }

    public boolean isEmailOnLeaveUpdate() {
        return emailOnLeaveUpdate;
    }

    public boolean isInAppOnServiceRequest() {
        return inAppOnServiceRequest;
    }

    public boolean isInAppOnStatusChange() {
        return inAppOnStatusChange;
    }

    public boolean isEmailMarketing() {
        return emailMarketing;
    }
}
