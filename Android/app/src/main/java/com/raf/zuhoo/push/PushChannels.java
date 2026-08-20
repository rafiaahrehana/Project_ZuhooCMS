package com.raf.zuhoo.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.raf.zuhoo.R;

// Android 8+ requires every notification to belong to a channel, and the channel is what the
// user actually controls. Grouping by what the notification is *about* — rather than one
// catch-all channel — lets someone silence billing reminders while keeping request updates.
public final class PushChannels {

    public static final String REQUESTS = "requests";
    public static final String BILLING = "billing";
    public static final String GENERAL = "general";

    private PushChannels() {
    }

    public static void createAll(Context context) {

        NotificationManager manager = context.getSystemService(NotificationManager.class);

        if (manager == null) {
            return;
        }

        manager.createNotificationChannel(channel(context, REQUESTS,
                R.string.channel_requests, NotificationManager.IMPORTANCE_HIGH));
        manager.createNotificationChannel(channel(context, BILLING,
                R.string.channel_billing, NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(channel(context, GENERAL,
                R.string.channel_general, NotificationManager.IMPORTANCE_DEFAULT));
    }

    private static NotificationChannel channel(Context context, String id, int nameRes, int importance) {
        return new NotificationChannel(id, context.getString(nameRes), importance);
    }

    // Maps the backend's NotificationType to a channel. Unknown values — the enum has grown
    // several times and will again — fall through to GENERAL rather than being dropped.
    public static String forType(String type) {

        if (type == null) {
            return GENERAL;
        }

        switch (type) {
            case "REQUEST_SUBMITTED":
            case "REQUEST_ASSIGNED":
            case "REQUEST_UPDATED":
            case "COMPLETED":
            case "REJECTED":
            case "CANCELLED":
            case "SLA_WARNING":
            case "SLA_BREACHED":
                return REQUESTS;

            case "PAYMENT_DUE":
            case "PAYMENT_RECEIVED":
            case "INVOICE_GENERATED":
            case "REFUND_PROCESSED":
            case "REFUND_REJECTED":
                return BILLING;

            default:
                return GENERAL;
        }
    }
}
