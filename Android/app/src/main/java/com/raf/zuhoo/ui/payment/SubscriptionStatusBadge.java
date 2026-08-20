package com.raf.zuhoo.ui.payment;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.SubscriptionStatus;

public final class SubscriptionStatusBadge {

    private SubscriptionStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (SubscriptionStatus.ACTIVE.equals(status)) {
            colorRes = R.color.status_success;
        } else if (SubscriptionStatus.EXPIRED.equals(status) || SubscriptionStatus.CANCELLED.equals(status)) {
            colorRes = R.color.status_danger;
        } else if (SubscriptionStatus.PENDING_PAYMENT.equals(status)) {
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
            case SubscriptionStatus.ACTIVE:
                return context.getString(R.string.subscription_status_active);
            case SubscriptionStatus.PENDING_PAYMENT:
                return context.getString(R.string.subscription_status_pending_payment);
            case SubscriptionStatus.EXPIRED:
                return context.getString(R.string.subscription_status_expired);
            case SubscriptionStatus.SUSPENDED:
                return context.getString(R.string.subscription_status_suspended);
            case SubscriptionStatus.CANCELLED:
                return context.getString(R.string.subscription_status_cancelled);
            default:
                return status;
        }
    }
}
