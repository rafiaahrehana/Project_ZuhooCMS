package com.raf.zuhoo.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.TextViewCompat;
import android.widget.TextView;

import com.raf.zuhoo.R;

/**
 * Renders the app's one status-badge look: a soft-tint pill (bg_badge_pill, tinted to ~12% of
 * the status color) with full-color text and a small tone icon, so status isn't color-only.
 *
 * Deliberately doesn't touch the 8 existing per-domain *StatusBadge classes
 * (LeaveRequestStatusBadge, ExpenseStatusBadge, ui.servicerequest.StatusBadge, etc.) — each
 * already resolves a status string to a @ColorInt via colorFor(Context, String) and a localized
 * label via labelFor(Context, String). This class is purely a shared renderer for that existing
 * output: callers keep calling their domain's colorFor()/labelFor() exactly as before and just
 * pass the result here instead of calling badge.setTextColor() directly.
 */
public final class StatusBadgeView {

    private StatusBadgeView() {
    }

    private static final int SOFT_ALPHA = 31; // ~12% of 255

    public static void bind(TextView badge, @ColorInt int color, String label) {
        badge.setText(label);
        badge.setTextColor(color);

        Context context = badge.getContext();

        // The pill background comes from Widget.Zuhoo.StatusBadge (applied via
        // style="@style/Widget.Zuhoo.StatusBadge" in the layout); this only re-tints it.
        int softBackground = ColorUtils.setAlphaComponent(color, SOFT_ALPHA);
        badge.setBackgroundTintList(ColorStateList.valueOf(softBackground));

        int iconRes = iconFor(context, color);
        if (iconRes != 0) {
            Drawable icon = ContextCompat.getDrawable(context, iconRes);
            if (icon != null) {
                icon = icon.mutate();
                DrawableCompat.setTint(icon, color);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        badge, icon, null, null, null);
            }
        } else {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    badge, null, null, null, null);
        }
    }

    /**
     * Maps the resolved status color back to one of the 4 shared tone families to pick an icon.
     * Comparing resolved ints is safe here — both sides come from the same 4 color resources
     * via ContextCompat.getColor() in the same context. Warning maps to "clock" rather than
     * "alert" because every domain currently uses status_warning exclusively for pending-style
     * states (PENDING, QUOTATION_PENDING, PENDING_PAYMENT...), never for a true warning.
     */
    @DrawableRes
    private static int iconFor(Context context, @ColorInt int color) {
        if (color == ContextCompat.getColor(context, R.color.status_success)) {
            return R.drawable.ic_check_circle;
        }
        if (color == ContextCompat.getColor(context, R.color.status_danger)) {
            return R.drawable.ic_cancel;
        }
        if (color == ContextCompat.getColor(context, R.color.status_warning)) {
            return R.drawable.ic_clock;
        }
        return 0; // status_info / anything else: text-only, no icon forced onto neutral states.
    }
}
