package com.raf.zuhoo.ui.payroll;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;

// Same two-static-method shape as ui.servicerequest.StatusBadge, for PayrollStatus
// (DRAFT, APPROVED, PAID, CANCELLED).
public final class PayrollStatusBadge {

    private PayrollStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if ("PAID".equals(status)) {
            colorRes = R.color.status_success;
        } else if ("CANCELLED".equals(status)) {
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
            case "DRAFT":
                return context.getString(R.string.payroll_status_draft);
            case "APPROVED":
                return context.getString(R.string.payroll_status_approved);
            case "PAID":
                return context.getString(R.string.payroll_status_paid);
            case "CANCELLED":
                return context.getString(R.string.status_cancelled);
            default:
                return status;
        }
    }
}
