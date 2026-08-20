package com.raf.zuhoo.ui.invoice;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.InvoiceStatus;

// Mirrors backend com.zuhoo.enums.InvoiceStatus:
// DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, VOIDED, REFUNDED.
public final class InvoiceStatusBadge {

    private InvoiceStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if ("PAID".equals(status)) {
            colorRes = R.color.status_success;
        } else if ("OVERDUE".equals(status) || "CANCELLED".equals(status) || "VOIDED".equals(status)) {
            colorRes = R.color.status_danger;
        } else if ("DRAFT".equals(status) || "ISSUED".equals(status) || "PARTIALLY_PAID".equals(status)) {
            colorRes = R.color.status_warning;
        } else {
            colorRes = R.color.status_info;
        }

        return ContextCompat.getColor(context, colorRes);
    }

    // Never render the raw wire constant — map it so it picks up the user's language.
    // Unknown values fall back to the constant rather than blanking out.
    public static String labelFor(Context context, String status) {

        if (status == null) {
            return "";
        }

        switch (status) {
            case InvoiceStatus.DRAFT:
                return context.getString(R.string.invoice_status_draft);
            case InvoiceStatus.ISSUED:
                return context.getString(R.string.invoice_status_issued);
            case InvoiceStatus.PARTIALLY_PAID:
                return context.getString(R.string.invoice_status_partially_paid);
            case InvoiceStatus.PAID:
                return context.getString(R.string.invoice_status_paid);
            case InvoiceStatus.OVERDUE:
                return context.getString(R.string.invoice_status_overdue);
            case InvoiceStatus.CANCELLED:
                return context.getString(R.string.invoice_status_cancelled);
            case InvoiceStatus.VOIDED:
                return context.getString(R.string.invoice_status_voided);
            case InvoiceStatus.REFUNDED:
                return context.getString(R.string.invoice_status_refunded);
            default:
                return status;
        }
    }
}
