package com.raf.zuhoo.ui.payment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.PaymentReceiptSummary;
import com.raf.zuhoo.databinding.ItemPaymentReceiptBinding;

import java.util.List;

public class PaymentReceiptAdapter extends RecyclerView.Adapter<PaymentReceiptAdapter.ViewHolder> {

    private final List<PaymentReceiptSummary> receipts;

    public PaymentReceiptAdapter(List<PaymentReceiptSummary> receipts) {
        this.receipts = receipts;
    }

    public void submitList(List<PaymentReceiptSummary> newReceipts) {
        receipts.clear();
        receipts.addAll(newReceipts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPaymentReceiptBinding binding = ItemPaymentReceiptBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(receipts.get(position));
    }

    @Override
    public int getItemCount() {
        return receipts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPaymentReceiptBinding binding;

        ViewHolder(ItemPaymentReceiptBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PaymentReceiptSummary receipt) {

            binding.itemReceiptNumber.setText(receipt.getReceiptNumber());
            binding.itemInvoiceNumber.setText(receipt.getInvoiceNumber());
            binding.itemAmount.setText(receipt.getAmount() != null
                    ? receipt.getAmount().toPlainString() : "-");
            binding.itemMeta.setText(receipt.getPaymentMethod() + " • " + receipt.getPaymentDate()
                    + " • " + receipt.getStatus());
        }
    }
}
