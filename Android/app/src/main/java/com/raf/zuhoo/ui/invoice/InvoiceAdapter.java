package com.raf.zuhoo.ui.invoice;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.databinding.ItemInvoiceBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(InvoiceSummary invoice);
    }

    private final List<InvoiceSummary> invoices;
    private final OnItemClickListener listener;

    public InvoiceAdapter(List<InvoiceSummary> invoices, OnItemClickListener listener) {
        this.invoices = invoices;
        this.listener = listener;
    }

    public void submitList(List<InvoiceSummary> newInvoices) {
        invoices.clear();
        invoices.addAll(newInvoices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInvoiceBinding binding = ItemInvoiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(invoices.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemInvoiceBinding binding;

        ViewHolder(ItemInvoiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(InvoiceSummary invoice, OnItemClickListener listener) {

            binding.itemInvoiceNumber.setText(invoice.getInvoiceNumber());
            binding.itemServiceRequestTitle.setText(invoice.getServiceRequestTitle());

            StatusBadgeView.bind(binding.itemStatusBadge,
                    InvoiceStatusBadge.colorFor(binding.getRoot().getContext(), invoice.getStatus()),
                    InvoiceStatusBadge.labelFor(binding.getRoot().getContext(), invoice.getStatus()));

            String currency = invoice.getCurrency() != null ? invoice.getCurrency() : "";
            String balance = invoice.getBalanceAmount() != null
                    ? invoice.getBalanceAmount().toPlainString() : "0";

            binding.itemBalance.setText(binding.getRoot().getContext().getString(
                    R.string.label_balance) + ": " + currency + " " + balance);

            binding.getRoot().setOnClickListener(v -> listener.onClick(invoice));
        }
    }
}
