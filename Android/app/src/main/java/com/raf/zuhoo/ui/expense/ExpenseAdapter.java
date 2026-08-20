package com.raf.zuhoo.ui.expense;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.databinding.ItemExpenseBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

// Shared by "My Expenses" and "Expense Approvals" — same reasoning as LeaveRequestAdapter: only
// whether the submitter's name shows differs, driven by a constructor flag.
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(ExpenseResponse expense);
    }

    private final List<ExpenseResponse> expenses;
    private final boolean showSubmittedBy;
    private final OnItemClickListener listener;

    public ExpenseAdapter(List<ExpenseResponse> expenses, boolean showSubmittedBy, OnItemClickListener listener) {
        this.expenses = expenses;
        this.showSubmittedBy = showSubmittedBy;
        this.listener = listener;
    }

    public void submitList(List<ExpenseResponse> newExpenses) {
        expenses.clear();
        expenses.addAll(newExpenses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExpenseBinding binding = ItemExpenseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(expenses.get(position), showSubmittedBy, listener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemExpenseBinding binding;

        ViewHolder(ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ExpenseResponse expense, boolean showSubmittedBy, OnItemClickListener listener) {

            android.content.Context context = binding.getRoot().getContext();

            binding.itemDescription.setText(expense.getDescription());
            binding.itemCategoryAndDate.setText(
                    ExpenseCategoryLabels.labelFor(context, expense.getCategory())
                            + " · " + expense.getExpenseDate());
            binding.itemAmount.setText(expense.getCurrency() + " " + expense.getAmount());

            StatusBadgeView.bind(binding.itemStatusBadge,
                    ExpenseStatusBadge.colorFor(context, expense.getStatus()),
                    ExpenseStatusBadge.labelFor(context, expense.getStatus()));

            binding.itemSubmittedByName.setVisibility(showSubmittedBy ? View.VISIBLE : View.GONE);
            if (showSubmittedBy) {
                binding.itemSubmittedByName.setText(expense.getSubmittedByName());
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(expense));
        }
    }
}
