package com.raf.zuhoo.ui.crm;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.LeadResponse;
import com.raf.zuhoo.databinding.ItemLeadBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

public class LeadAdapter extends RecyclerView.Adapter<LeadAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(LeadResponse lead);
    }

    private final List<LeadResponse> leads;
    private final OnItemClickListener listener;

    public LeadAdapter(List<LeadResponse> leads, OnItemClickListener listener) {
        this.leads = leads;
        this.listener = listener;
    }

    public void submitList(List<LeadResponse> newLeads) {
        leads.clear();
        leads.addAll(newLeads);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeadBinding binding = ItemLeadBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(leads.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return leads.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLeadBinding binding;

        ViewHolder(ItemLeadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LeadResponse lead, OnItemClickListener listener) {

            android.content.Context context = binding.getRoot().getContext();

            binding.itemContactName.setText(lead.getContactName());
            binding.itemCompanyName.setText(lead.getCompanyName());
            binding.itemPhone.setText(lead.getPhone());

            StatusBadgeView.bind(binding.itemStatusBadge,
                    LeadStatusBadge.colorFor(context, lead.getStatus()),
                    LeadStatusBadge.labelFor(context, lead.getStatus()));

            binding.getRoot().setOnClickListener(v -> listener.onClick(lead));
        }
    }
}
