package com.raf.zuhoo.push;

import android.content.Context;
import android.content.Intent;

import com.raf.zuhoo.ui.dashboard.DashboardActivity;
import com.raf.zuhoo.ui.invoice.InvoiceListActivity;
import com.raf.zuhoo.ui.notification.NotificationListActivity;
import com.raf.zuhoo.ui.servicerequest.ServiceRequestDetailActivity;

// Decides where tapping a notification lands the user.
//
// The whole point of a push is to take someone straight to the thing that needs them, so a
// request update opens that request rather than dumping them on a list to hunt for it.
public final class PushRouter {

    private PushRouter() {
    }

    public static Intent intentFor(Context context, String type, String serviceRequestId) {

        Intent target = resolve(context, type, serviceRequestId);

        // The app may be cold-started from the notification, in which case there is no back
        // stack to return to — send Back to the dashboard instead of straight out of the app.
        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return target;
    }

    private static Intent resolve(Context context, String type, String serviceRequestId) {

        Long requestId = parseId(serviceRequestId);

        if (requestId != null && PushChannels.REQUESTS.equals(PushChannels.forType(type))) {
            Intent intent = new Intent(context, ServiceRequestDetailActivity.class);
            intent.putExtra(ServiceRequestDetailActivity.EXTRA_REQUEST_ID, requestId);
            return intent;
        }

        if (PushChannels.BILLING.equals(PushChannels.forType(type))) {
            return new Intent(context, InvoiceListActivity.class);
        }

        // Anything else: the notification centre, which shows the message in full.
        return new Intent(context, type == null
                ? DashboardActivity.class : NotificationListActivity.class);
    }

    private static Long parseId(String value) {

        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
