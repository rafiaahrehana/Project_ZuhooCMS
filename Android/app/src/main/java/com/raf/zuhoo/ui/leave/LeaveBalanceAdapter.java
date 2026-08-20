package com.raf.zuhoo.ui.leave;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.LeaveBalanceResponse;
import com.raf.zuhoo.databinding.ItemLeaveBalanceBinding;

import java.util.List;

public class LeaveBalanceAdapter extends RecyclerView.Adapter<LeaveBalanceAdapter.ViewHolder> {

    private final List<LeaveBalanceResponse> balances;

    public LeaveBalanceAdapter(List<LeaveBalanceResponse> balances) {
        this.balances = balances;
    }

    public void submitList(List<LeaveBalanceResponse> newBalances) {
        balances.clear();
        balances.addAll(newBalances);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeaveBalanceBinding binding = ItemLeaveBalanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(balances.get(position));
    }

    @Override
    public int getItemCount() {
        return balances.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLeaveBalanceBinding binding;

        ViewHolder(ItemLeaveBalanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LeaveBalanceResponse balance) {

            android.content.Context context = binding.getRoot().getContext();

            binding.balanceLeaveType.setText(LeaveTypeLabels.labelFor(context, balance.getLeaveType()));
            binding.balanceDays.setText(context.getString(R.string.leave_balance_format,
                    balance.getRemainingDays(), balance.getEntitledDays()));
        }
    }
}
