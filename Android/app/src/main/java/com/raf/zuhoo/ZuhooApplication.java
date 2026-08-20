package com.raf.zuhoo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.raf.zuhoo.di.AppGraph;
import com.raf.zuhoo.push.PushChannels;

// Exists so the things that must be single-instance for the whole process — the encrypted token
// store, the Room cache, the chat socket — have an owner with a process-long lifetime, instead of
// each Activity constructing its own.
public class ZuhooApplication extends Application {

    private static ZuhooApplication instance;

    private AppGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        graph = new AppGraph(this);

        // Channels must exist before the first notification is posted, and creating them is
        // idempotent — doing it at startup means a cold start from a push still has them.
        PushChannels.createAll(this);

        // On this Android version the decor no longer reserves space for a classic ActionBar —
        // android:id/content already starts below the status bar (that part the system does
        // handle), but the ActionBar itself is then drawn as an overlay on top of that content
        // instead of pushing it down, clipping every screen's title/header row. Measured via
        // uiautomator: content top = statusBarHeight, ActionBar spans statusBarHeight-ish to
        // +actionBarSize — so content needs an extra actionBarSize of top padding, on top of
        // whatever system-bar inset applies. Fixed once here rather than in every Activity.
        registerActivityLifecycleCallbacks(new EdgeToEdgeContentPadder());
    }

    private static final class EdgeToEdgeContentPadder implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            View content = activity.findViewById(android.R.id.content);

            int actionBarHeight = 0;
            if (activity instanceof AppCompatActivity) {
                ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
                if (actionBar != null && actionBar.isShowing()) {
                    TypedValue tv = new TypedValue();
                    if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                        actionBarHeight = TypedValue.complexToDimensionPixelSize(
                                tv.data, activity.getResources().getDisplayMetrics());
                    }
                }
            }

            int extraTopPadding = actionBarHeight;
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), bars.top + extraTopPadding, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(content);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }

    public static AppGraph graph() {
        return instance.graph;
    }
}
