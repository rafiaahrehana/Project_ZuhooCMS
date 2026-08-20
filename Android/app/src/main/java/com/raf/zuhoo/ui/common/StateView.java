package com.raf.zuhoo.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.raf.zuhoo.R;

/**
 * The one loading/empty/error treatment for list (and other data-driven) screens, replacing the
 * old pattern of a screen managing its own bare "emptyState" TextView + ProgressBar directly.
 *
 * Usage: add a single {@code <com.raf.zuhoo.ui.common.StateView android:id="@+id/stateView" .../>}
 * as a sibling of the screen's RecyclerView inside their shared FrameLayout, then drive it from
 * the same CachedListViewModel LiveData the screen already observes — this changes how that
 * state renders, not how it's tracked:
 *
 * <pre>
 * viewModel.loading().observe(this, loading -> {
 *     if (loading) stateView.showLoading();
 * });
 * viewModel.items().observe(this, items -> {
 *     if (items.isEmpty()) {
 *         stateView.showEmpty(R.drawable.ic_inbox, R.string.empty_leave_title, R.string.empty_leave_subtitle);
 *     } else {
 *         stateView.showContent();
 *     }
 *     adapter.submitList(items);
 * });
 * viewModel.error().observe(this, event -> {
 *     Integer messageRes = event.getContentIfNotHandled();
 *     if (messageRes != null) {
 *         stateView.showError(messageRes, () -> viewModel.refresh());
 *     }
 * });
 * </pre>
 */
public class StateView extends FrameLayout {

    private CircularProgressIndicator loadingIndicator;
    private View messageGroup;
    private ImageView icon;
    private TextView title;
    private TextView subtitle;
    private MaterialButton action;

    public StateView(Context context) {
        super(context);
        init();
    }

    public StateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_state, this, true);
        loadingIndicator = findViewById(R.id.stateLoading);
        messageGroup = findViewById(R.id.stateMessageGroup);
        icon = findViewById(R.id.stateIcon);
        title = findViewById(R.id.stateTitle);
        subtitle = findViewById(R.id.stateSubtitle);
        action = findViewById(R.id.stateAction);
        setVisibility(GONE);
    }

    /** Hides this view entirely so the screen's real content (RecyclerView, etc.) shows through. */
    public void showContent() {
        setVisibility(GONE);
    }

    public void showLoading() {
        setVisibility(VISIBLE);
        loadingIndicator.setVisibility(VISIBLE);
        messageGroup.setVisibility(GONE);
    }

    public void showEmpty(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int subtitleRes) {
        showMessage(iconRes, getContext().getString(titleRes), getContext().getString(subtitleRes), 0, null);
    }

    public void showEmpty(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int subtitleRes,
                           @StringRes int actionTextRes, OnClickListener onAction) {
        showMessage(iconRes, getContext().getString(titleRes), getContext().getString(subtitleRes),
                actionTextRes, onAction);
    }

    /** Error state defaults to a "Try Again" action wired to the screen's own refresh(). */
    public void showError(@StringRes int subtitleRes, OnClickListener onRetry) {
        showMessage(R.drawable.ic_alert, getContext().getString(R.string.state_error_title),
                getContext().getString(subtitleRes), R.string.state_action_try_again, onRetry);
    }

    private void showMessage(@DrawableRes int iconRes, String titleText, @Nullable String subtitleText,
                              @StringRes int actionTextRes, @Nullable OnClickListener onAction) {
        setVisibility(VISIBLE);
        loadingIndicator.setVisibility(GONE);
        messageGroup.setVisibility(VISIBLE);

        icon.setImageResource(iconRes);
        title.setText(titleText);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            subtitle.setText(subtitleText);
            subtitle.setVisibility(VISIBLE);
        } else {
            subtitle.setVisibility(GONE);
        }

        if (actionTextRes != 0 && onAction != null) {
            action.setText(actionTextRes);
            action.setOnClickListener(onAction);
            action.setVisibility(VISIBLE);
        } else {
            action.setVisibility(GONE);
        }
    }
}
