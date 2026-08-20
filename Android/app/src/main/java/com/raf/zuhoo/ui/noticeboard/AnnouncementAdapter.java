package com.raf.zuhoo.ui.noticeboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.AnnouncementResponse;
import com.raf.zuhoo.databinding.ItemAnnouncementBinding;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(AnnouncementResponse announcement);
    }

    private final List<AnnouncementResponse> announcements;
    private final OnItemClickListener listener;

    public AnnouncementAdapter(List<AnnouncementResponse> announcements, OnItemClickListener listener) {
        this.announcements = announcements;
        this.listener = listener;
    }

    public void submitList(List<AnnouncementResponse> newAnnouncements) {
        announcements.clear();
        announcements.addAll(newAnnouncements);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAnnouncementBinding binding = ItemAnnouncementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(announcements.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAnnouncementBinding binding;

        ViewHolder(ItemAnnouncementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AnnouncementResponse announcement, OnItemClickListener listener) {

            binding.itemTitle.setText(announcement.getTitle());
            binding.itemBody.setText(announcement.getBody());
            binding.itemPublishedAt.setText(announcement.getPublishedAt());

            binding.getRoot().setOnClickListener(v -> listener.onClick(announcement));
        }
    }
}
