package com.raf.zuhoo.ui.support;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.TicketStatus;

public final class TicketStatusBadge {

    private TicketStatusBadge() {
    }

    public static int colorFor(Context context, String status) {

        int colorRes;

        if (TicketStatus.RESOLVED.equals(status)) {
            colorRes = R.color.status_success;
        } else if ("NEW".equals(status)) {
            colorRes = R.color.status_warning;
        } else if (TicketStatus.CLOSED.equals(status)) {
            colorRes = R.color.status_danger;
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
            case TicketStatus.NEW:
                return context.getString(R.string.ticket_status_new);
            case TicketStatus.OPEN:
                return context.getString(R.string.ticket_status_open);
            case TicketStatus.IN_PROGRESS:
                return context.getString(R.string.ticket_status_in_progress);
            case TicketStatus.WAITING:
                return context.getString(R.string.ticket_status_waiting);
            case TicketStatus.ON_HOLD:
                return context.getString(R.string.ticket_status_on_hold);
            case TicketStatus.RESOLVED:
                return context.getString(R.string.ticket_status_resolved);
            case TicketStatus.CLOSED:
                return context.getString(R.string.ticket_status_closed);
            case TicketStatus.REOPENED:
                return context.getString(R.string.ticket_status_reopened);
            default:
                return status;
        }
    }
}
