package com.raf.zuhoo.ui.notification;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.NotificationResponse;
import com.raf.zuhoo.databinding.ActivityNotificationListBinding;
import com.raf.zuhoo.ui.common.CacheStamp;

import java.util.ArrayList;

public class NotificationListActivity extends AppCompatActivity {

    private ActivityNotificationListBinding binding;
    private NotificationListViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNotificationListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NotificationListViewModel.class);

        adapter = new NotificationAdapter(new ArrayList<>(), this::openNotification);
        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.notificationsRecyclerView.setAdapter(adapter);

        binding.btnMarkAllRead.setOnClickListener(v -> viewModel.markAllRead());

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.start();
    }

    private void observeViewModel() {

        viewModel.items().observe(this, notifications -> {
            adapter.submitList(notifications);
            if (notifications.isEmpty()) {
                binding.stateView.showEmpty(R.drawable.ic_notifications,
                        R.string.empty_notifications, R.string.empty_notifications_subtitle);
            } else {
                binding.stateView.showContent();
            }
        });

        viewModel.loading().observe(this, loading -> {
            if (loading && adapter.getItemCount() == 0) {
                binding.stateView.showLoading();
            }
        });

        viewModel.showingCached().observe(this, cached ->
                CacheStamp.bind(binding.cacheStamp, cached, viewModel.lastUpdated().getValue()));

        // Only fires when there's nothing cached to fall back on (see CachedListViewModel) — a
        // background refresh failing with content already on screen never reaches here.
        viewModel.error().observe(this, event -> {
            Integer messageRes = event.consume();
            if (messageRes != null) {
                binding.stateView.showError(messageRes, v -> viewModel.refresh());
            }
        });
    }

    private void openNotification(NotificationResponse notification) {
        if (!notification.isRead()) {
            viewModel.markRead(notification.getId());
        }
    }
}
