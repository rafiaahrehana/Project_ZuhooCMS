package com.raf.zuhoo.ui.common;

import android.app.Activity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;

import retrofit2.Response;

// One place that decides how a failed call is shown to the user.
public final class UiErrors {

    private UiErrors() {
    }

    public static void show(Activity activity, Response<?> response, String fallback) {
        show(activity, ApiErrors.describe(response, fallback));
    }

    // Overload for ViewModels: the error body is a one-shot stream, so a VM resolves it to an
    // ApiError at the point of failure and passes that on rather than a spent Response.
    public static void show(Activity activity, ApiErrors.ApiError error) {

        // A lapsed trial blocks every write across the whole app, not just this one action, so
        // it gets a dialog the user has to acknowledge rather than a toast that slides away and
        // leaves them retrying a button that can't work.
        if (error.isSubscriptionExpired()) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.error_subscription_expired_title)
                    .setMessage(error.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_LONG).show();
    }
}
