package com.raf.zuhoo.ui.wallet;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.WalletTransactionResponse;
import com.raf.zuhoo.databinding.ItemWalletTransactionBinding;

import java.math.BigDecimal;
import java.util.List;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private final List<WalletTransactionResponse> transactions;

    public WalletTransactionAdapter(List<WalletTransactionResponse> transactions) {
        this.transactions = transactions;
    }

    public void submitList(List<WalletTransactionResponse> newTransactions) {
        transactions.clear();
        transactions.addAll(newTransactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalletTransactionBinding binding = ItemWalletTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWalletTransactionBinding binding;

        ViewHolder(ItemWalletTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WalletTransactionResponse transaction) {

            android.content.Context context = binding.getRoot().getContext();

            binding.itemType.setText(transaction.getType());
            binding.itemReference.setText(transaction.getReference());
            binding.itemTransactedAt.setText(transaction.getTransactedAt());

            BigDecimal amount = transaction.getAmount();
            boolean negative = amount != null && amount.signum() < 0;
            binding.itemAmount.setText(String.valueOf(amount));
            binding.itemAmount.setTextColor(ContextCompat.getColor(context,
                    negative ? R.color.status_danger : R.color.status_success));
        }
    }
}
