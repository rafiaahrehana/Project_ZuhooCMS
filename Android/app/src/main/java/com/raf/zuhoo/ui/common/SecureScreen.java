package com.raf.zuhoo.ui.common;

import android.app.Activity;
import android.view.WindowManager;

/**
 * Blocks screenshots and keeps a screen out of the recent-apps thumbnail.
 *
 * Applied to anything showing money or personal details. The recents thumbnail is the part people
 * forget: without this, an invoice or a profile stays rendered in the app switcher after the user
 * has moved on, visible to anyone who picks up the phone.
 */
public final class SecureScreen {

    private SecureScreen() {
    }

    public static void apply(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
