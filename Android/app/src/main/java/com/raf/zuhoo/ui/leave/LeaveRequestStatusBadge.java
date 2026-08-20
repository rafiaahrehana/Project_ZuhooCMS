package com.raf.zuhoo.ui.leave;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.LeaveRequestStatus;

// Same two-static-method shape as ui.servicerequest.StatusBadge — each domain gets its own
// sibling rather than a shared generic badge, since the status sets and colors don't line up.
public final class LeaveRequestStatusBadge {

    private LeaveRequestStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (LeaveRequestStatus.APPROVED.equals(status)) {
            colorRes = R.color.status_success;
        } else if (LeaveRequestStatus.REJECTED.equals(status)
                || LeaveRequestStatus.CANCELLED.equals(status)) {
            colorRes = R.color.status_danger;
        } else {
            colorRes = R.color.status_warning;
        }

        return ContextCompat.getColor(context, colorRes);
    }

    public static String labelFor(Context context, String status) {

        if (status == null) {
            return "";
        }

        switch (status) {
            case LeaveRequestStatus.PENDING:
                return context.getString(R.string.status_pending);
            case LeaveRequestStatus.APPROVED:
                return context.getString(R.string.status_approved);
            case LeaveRequestStatus.REJECTED:
                return context.getString(R.string.status_rejected);
            case LeaveRequestStatus.CANCELLED:
                return context.getString(R.string.status_cancelled);
            default:
                return status;
        }
    }
}
