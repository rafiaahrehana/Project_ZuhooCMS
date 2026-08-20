package com.raf.zuhoo.ui.catalog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.CompanyServiceResponse;
import com.raf.zuhoo.databinding.ItemCatalogHeaderBinding;
import com.raf.zuhoo.databinding.ItemCatalogServiceBinding;

import java.util.List;

public class CatalogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SERVICE = 1;

    public interface OnRequestClickListener {
        void onRequest(CompanyServiceResponse service);
    }

    private final List<CatalogRow> rows;
    private final OnRequestClickListener listener;

    public CatalogAdapter(List<CatalogRow> rows, OnRequestClickListener listener) {
        this.rows = rows;
        this.listener = listener;
    }

    public void submitList(List<CatalogRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader() ? TYPE_HEADER : TYPE_SERVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            ItemCatalogHeaderBinding binding = ItemCatalogHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new HeaderViewHolder(binding);
        }

        ItemCatalogServiceBinding binding = ItemCatalogServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        CatalogRow row = rows.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(row);
        } else {
            ((ServiceViewHolder) holder).bind(row, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final ItemCatalogHeaderBinding binding;

        HeaderViewHolder(ItemCatalogHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CatalogRow row) {
            binding.getRoot().setText(row.getHeaderName());
        }
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {

        private final ItemCatalogServiceBinding binding;

        ServiceViewHolder(ItemCatalogServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CatalogRow row, OnRequestClickListener listener) {

            CompanyServiceResponse service = row.getService();

            binding.itemServiceName.setText(service.getName());
            binding.itemServiceDescription.setText(service.getDescription());
            binding.itemServiceDescription.setVisibility(
                    service.getDescription() == null || service.getDescription().isEmpty()
                            ? android.view.View.GONE : android.view.View.VISIBLE);

            String currency = service.getCurrency() != null ? service.getCurrency() : "";
            binding.itemServicePrice.setText(service.getPrice() != null
                    ? currency + " " + service.getPrice() : "");

            binding.btnRequestService.setOnClickListener(v -> listener.onRequest(service));
        }
    }
}
