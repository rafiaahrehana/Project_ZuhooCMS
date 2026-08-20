package com.raf.zuhoo.ui.support;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.databinding.ItemSupportTicketBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

public class SupportTicketAdapter extends RecyclerView.Adapter<SupportTicketAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(SupportTicketResponse ticket);
    }

    private final List<SupportTicketResponse> tickets;
    private final OnItemClickListener listener;

    public SupportTicketAdapter(List<SupportTicketResponse> tickets, OnItemClickListener listener) {
        this.tickets = tickets;
        this.listener = listener;
    }

    public void submitList(List<SupportTicketResponse> newTickets) {
        tickets.clear();
        tickets.addAll(newTickets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSupportTicketBinding binding = ItemSupportTicketBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(tickets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSupportTicketBinding binding;

        ViewHolder(ItemSupportTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SupportTicketResponse ticket, OnItemClickListener listener) {

            binding.itemTicketTitle.setText(ticket.getTitle());
            binding.itemTicketNumber.setText(ticket.getTicketNumber());
            binding.itemTicketPriority.setText(ticket.getPriority());

            StatusBadgeView.bind(binding.itemStatusBadge,
                    TicketStatusBadge.colorFor(binding.getRoot().getContext(), ticket.getStatus()),
                    TicketStatusBadge.labelFor(binding.getRoot().getContext(), ticket.getStatus()));

            binding.getRoot().setOnClickListener(v -> listener.onClick(ticket));
        }
    }
}
