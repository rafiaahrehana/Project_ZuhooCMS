package com.raf.zuhoo.ui.search;

import android.content.Context;

import com.raf.zuhoo.R;

public final class SearchResultTypeLabels {

    private SearchResultTypeLabels() {
    }

    public static String labelFor(Context context, String type) {

        if (type == null) {
            return "";
        }

        switch (type) {
            case "LEAD":
                return context.getString(R.string.search_type_lead);
            case "CLIENT":
                return context.getString(R.string.search_type_client);
            case "OPPORTUNITY":
                return context.getString(R.string.search_type_opportunity);
            case "SERVICE_REQUEST":
                return context.getString(R.string.search_type_service_request);
            case "TICKET":
                return context.getString(R.string.search_type_ticket);
            case "INVOICE":
                return context.getString(R.string.search_type_invoice);
            default:
                return type;
        }
    }
}
