package com.raf.zuhoo.ui.support;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.SupportMessageResponse;
import com.raf.zuhoo.databinding.ItemSupportMessageBinding;

import java.util.List;

public class SupportMessageAdapter extends RecyclerView.Adapter<SupportMessageAdapter.ViewHolder> {

    private final List<SupportMessageResponse> messages;

    public SupportMessageAdapter(List<SupportMessageResponse> messages) {
        this.messages = messages;
    }

    public void submitList(List<SupportMessageResponse> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    // Socket-pushed messages can collide with ones already loaded over REST (a reconnect refetch,
    // or a second session of the same user), so match on the server id before appending rather
    // than showing the same message twice.
    public void addMessage(SupportMessageResponse message) {

        int existing = indexOf(message.getId());

        if (existing >= 0) {
            messages.set(existing, message);
            notifyItemChanged(existing);
            return;
        }

        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private int indexOf(Long id) {

        if (id == null) {
            return -1;
        }

        for (int i = 0; i < messages.size(); i++) {
            if (id.equals(messages.get(i).getId())) {
                return i;
            }
        }

        return -1;
    }

    public int getMessageCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSupportMessageBinding binding = ItemSupportMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSupportMessageBinding binding;

        ViewHolder(ItemSupportMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupportMessageResponse message) {
            binding.messageAuthor.setText(message.getSentByName());
            binding.messageContent.setText(message.getMessage());
            binding.messageDate.setText(message.getCreatedAt());
        }
    }
}
