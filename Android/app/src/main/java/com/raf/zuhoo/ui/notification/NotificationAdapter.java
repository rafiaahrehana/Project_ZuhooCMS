package com.raf.zuhoo.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.NotificationResponse;
import com.raf.zuhoo.databinding.ItemNotificationBinding;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(NotificationResponse notification);
    }

    private final List<NotificationResponse> notifications;
    private final OnItemClickListener listener;

    public NotificationAdapter(List<NotificationResponse> notifications, OnItemClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void submitList(List<NotificationResponse> newNotifications) {
        notifications.clear();
        notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotificationBinding binding;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationResponse notification, OnItemClickListener listener) {

            binding.itemTitle.setText(notification.getTitle());
            binding.itemMessage.setText(notification.getMessage());
            binding.itemDate.setText(notification.getCreatedAt());
            binding.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            binding.getRoot().setOnClickListener(v -> listener.onClick(notification));
        }
    }
}
