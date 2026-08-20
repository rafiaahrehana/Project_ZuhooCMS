package com.raf.zuhoo.ui.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.ServicePackageResponse;
import com.raf.zuhoo.databinding.ItemPackageBinding;

import java.util.List;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.ViewHolder> {

    public interface OnSubscribeClickListener {
        void onSubscribe(ServicePackageResponse servicePackage);
    }

    private final List<ServicePackageResponse> packages;
    private final OnSubscribeClickListener listener;

    public PackageAdapter(List<ServicePackageResponse> packages, OnSubscribeClickListener listener) {
        this.packages = packages;
        this.listener = listener;
    }

    public void submitList(List<ServicePackageResponse> newPackages) {
        packages.clear();
        packages.addAll(newPackages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPackageBinding binding = ItemPackageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(packages.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPackageBinding binding;

        ViewHolder(ItemPackageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServicePackageResponse servicePackage, OnSubscribeClickListener listener) {

            binding.itemPackageName.setText(servicePackage.getName());
            binding.itemPackageDescription.setText(servicePackage.getDescription());
            binding.itemPackageDescription.setVisibility(
                    servicePackage.getDescription() == null || servicePackage.getDescription().isEmpty()
                            ? View.GONE : View.VISIBLE);

            String price = servicePackage.getEffectivePrice() != null
                    ? servicePackage.getEffectivePrice().toPlainString() : "-";
            binding.itemPackagePrice.setText(price + " / " + servicePackage.getBillingCycle());

            binding.btnSubscribe.setOnClickListener(v -> listener.onSubscribe(servicePackage));
        }
    }
}
