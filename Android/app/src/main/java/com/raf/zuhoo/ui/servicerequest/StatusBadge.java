package com.raf.zuhoo.ui.servicerequest;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.ServiceRequestStatus;

public final class StatusBadge {

    private StatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (ServiceRequestStatus.COMPLETED.equals(status)) {
            colorRes = R.color.status_success;
        } else if (ServiceRequestStatus.REJECTED.equals(status)
                || ServiceRequestStatus.CANCELLED.equals(status)) {
            colorRes = R.color.status_danger;
        } else if (ServiceRequestStatus.PENDING.equals(status)
                || ServiceRequestStatus.QUOTATION_PENDING.equals(status)) {
            colorRes = R.color.status_warning;
        } else {
            colorRes = R.color.status_info;
        }

        return ContextCompat.getColor(context, colorRes);
    }

    // Statuses arrive as stable English constants ("IN_PROGRESS"). Never show one raw — map it
    // to a string resource so it picks up the user's chosen language. Unknown values (the
    // backend can add enum constants) fall back to the wire value rather than blanking out.
    public static String labelFor(Context context, String status) {

        if (status == null) {
            return "";
        }

        switch (status) {
            case ServiceRequestStatus.PENDING:
                return context.getString(R.string.status_pending);
            case ServiceRequestStatus.QUOTATION_PENDING:
                return context.getString(R.string.status_quotation_pending);
            case ServiceRequestStatus.ASSIGNED:
                return context.getString(R.string.status_assigned);
            case ServiceRequestStatus.IN_PROGRESS:
                return context.getString(R.string.status_in_progress);
            case ServiceRequestStatus.WAITING_CLIENT:
                return context.getString(R.string.status_waiting_client);
            case ServiceRequestStatus.UNDER_REVIEW:
                return context.getString(R.string.status_under_review);
            case ServiceRequestStatus.COMPLETED:
                return context.getString(R.string.status_completed);
            case ServiceRequestStatus.REJECTED:
                return context.getString(R.string.status_rejected);
            case ServiceRequestStatus.CANCELLED:
                return context.getString(R.string.status_cancelled);
            case ServiceRequestStatus.RESUBMITTED:
                return context.getString(R.string.status_resubmitted);
            default:
                return status;
        }
    }
}
