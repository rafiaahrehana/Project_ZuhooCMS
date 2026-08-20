package com.raf.zuhoo.ui.payment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.SubscriptionStatus;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;
import com.raf.zuhoo.databinding.ItemSubscriptionBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {

    public interface Listener {
        void onPayNow(SubscriptionSummary subscription);
        void onCancel(SubscriptionSummary subscription);
    }

    private final List<SubscriptionSummary> subscriptions;
    private final Listener listener;

    public SubscriptionAdapter(List<SubscriptionSummary> subscriptions, Listener listener) {
        this.subscriptions = subscriptions;
        this.listener = listener;
    }

    public void submitList(List<SubscriptionSummary> newSubscriptions) {
        subscriptions.clear();
        subscriptions.addAll(newSubscriptions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubscriptionBinding binding = ItemSubscriptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(subscriptions.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return subscriptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSubscriptionBinding binding;

        ViewHolder(ItemSubscriptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SubscriptionSummary subscription, Listener listener) {

            binding.itemPackageName.setText(subscription.getPackageName());
            StatusBadgeView.bind(binding.itemStatusBadge,
                    SubscriptionStatusBadge.colorFor(binding.getRoot().getContext(), subscription.getStatus()),
                    SubscriptionStatusBadge.labelFor(binding.getRoot().getContext(), subscription.getStatus()));

            binding.itemMeta.setText(subscription.getPricePaid() + " • " + subscription.getBillingCycle());
            binding.itemQuota.setText(subscription.getRequestsUsed() + " / " + subscription.getRequestQuota()
                    + " requests used");

            boolean pendingPayment = SubscriptionStatus.PENDING_PAYMENT.equals(subscription.getStatus());
            boolean cancellable = SubscriptionStatus.ACTIVE.equals(subscription.getStatus()) || pendingPayment;

            binding.btnPayNow.setVisibility(pendingPayment ? View.VISIBLE : View.GONE);
            binding.btnCancelSubscription.setVisibility(cancellable ? View.VISIBLE : View.GONE);

            binding.btnPayNow.setOnClickListener(v -> listener.onPayNow(subscription));
            binding.btnCancelSubscription.setOnClickListener(v -> listener.onCancel(subscription));
        }
    }
}
