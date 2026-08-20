package com.raf.zuhoo.ui.servicerequest;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.databinding.ItemServiceRequestBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

public class ServiceRequestAdapter extends RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(ServiceRequestSummary request);
    }

    private final List<ServiceRequestSummary> requests;
    private final OnItemClickListener listener;

    public ServiceRequestAdapter(List<ServiceRequestSummary> requests, OnItemClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    public void submitList(List<ServiceRequestSummary> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceRequestBinding binding = ItemServiceRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemServiceRequestBinding binding;

        ViewHolder(ItemServiceRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServiceRequestSummary request, OnItemClickListener listener) {

            binding.itemTitle.setText(request.getTitle());
            binding.itemHubService.setText(request.getHubServiceName());
            binding.itemPriority.setText(request.getPriority());

            StatusBadgeView.bind(binding.itemStatusBadge,
                    StatusBadge.colorFor(binding.getRoot().getContext(), request.getStatus()),
                    StatusBadge.labelFor(binding.getRoot().getContext(), request.getStatus()));

            binding.getRoot().setOnClickListener(v -> listener.onClick(request));
        }
    }
}
