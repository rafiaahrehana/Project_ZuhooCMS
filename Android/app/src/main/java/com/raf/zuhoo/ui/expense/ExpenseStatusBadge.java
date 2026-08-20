package com.raf.zuhoo.ui.expense;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.ExpenseStatus;

// Same two-static-method shape as ui.servicerequest.StatusBadge.
public final class ExpenseStatusBadge {

    private ExpenseStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (ExpenseStatus.APPROVED.equals(status) || ExpenseStatus.PAID.equals(status)) {
            colorRes = R.color.status_success;
        } else if (ExpenseStatus.REJECTED.equals(status) || ExpenseStatus.CANCELLED.equals(status)) {
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
            case ExpenseStatus.PENDING:
                return context.getString(R.string.status_pending);
            case ExpenseStatus.APPROVED:
                return context.getString(R.string.status_approved);
            case ExpenseStatus.REJECTED:
                return context.getString(R.string.status_rejected);
            case ExpenseStatus.PAID:
                return context.getString(R.string.expense_status_paid);
            case ExpenseStatus.CANCELLED:
                return context.getString(R.string.status_cancelled);
            default:
                return status;
        }
    }
}
