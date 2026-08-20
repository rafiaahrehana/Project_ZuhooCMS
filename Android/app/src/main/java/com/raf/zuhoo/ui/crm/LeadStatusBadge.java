package com.raf.zuhoo.ui.crm;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.LeadStatus;

// Same two-static-method shape as ui.servicerequest.StatusBadge.
public final class LeadStatusBadge {

    private LeadStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (LeadStatus.QUALIFIED.equals(status)) {
            colorRes = R.color.status_success;
        } else if (LeadStatus.DISQUALIFIED.equals(status)) {
            colorRes = R.color.status_danger;
        } else if (LeadStatus.CONTACTED.equals(status)) {
            colorRes = R.color.status_warning;
        } else {
            colorRes = R.color.status_info;
        }

        return ContextCompat.getColor(context, colorRes);
    }

    public static String labelFor(Context context, String status) {

        if (status == null) {
            return "";
        }

        switch (status) {
            case LeadStatus.NEW:
                return context.getString(R.string.lead_status_new);
            case LeadStatus.CONTACTED:
                return context.getString(R.string.lead_status_contacted);
            case LeadStatus.QUALIFIED:
                return context.getString(R.string.lead_status_qualified);
            case LeadStatus.DISQUALIFIED:
                return context.getString(R.string.lead_status_disqualified);
            default:
                return status;
        }
    }
}
