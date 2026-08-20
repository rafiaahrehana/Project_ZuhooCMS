package com.raf.zuhoo.ui.common;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.raf.zuhoo.R;

// Renders the "showing saved data from …" line that list screens put above their content when
// the rows came from the cache instead of a live response.
public final class CacheStamp {

    private CacheStamp() {
    }

    public static void bind(TextView view, boolean showingCached, Long updatedAt) {

        if (!showingCached || updatedAt == null) {
            view.setVisibility(View.GONE);
            return;
        }

        Context context = view.getContext();

        CharSequence relative = DateUtils.getRelativeTimeSpanString(
                updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);

        view.setText(context.getString(R.string.cache_last_updated, relative));
        view.setVisibility(View.VISIBLE);
    }
}
