package com.raf.zuhoo.ui.payroll;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.PayrollResponse;
import com.raf.zuhoo.databinding.ItemPayslipBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.text.DateFormatSymbols;
import java.util.List;

public class PayslipAdapter extends RecyclerView.Adapter<PayslipAdapter.ViewHolder> {

    public interface OnDownloadClickListener {
        void onDownload(PayrollResponse payslip);
    }

    private final List<PayrollResponse> payslips;
    private final OnDownloadClickListener listener;

    public PayslipAdapter(List<PayrollResponse> payslips, OnDownloadClickListener listener) {
        this.payslips = payslips;
        this.listener = listener;
    }

    public void submitList(List<PayrollResponse> newPayslips) {
        payslips.clear();
        payslips.addAll(newPayslips);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPayslipBinding binding = ItemPayslipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(payslips.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return payslips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPayslipBinding binding;

        ViewHolder(ItemPayslipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PayrollResponse payslip, OnDownloadClickListener listener) {

            android.content.Context context = binding.getRoot().getContext();

            String monthName = new DateFormatSymbols().getMonths()[payslip.getPayMonth() - 1];
            binding.itemPayPeriod.setText(monthName + " " + payslip.getPayYear());

            binding.itemNetSalary.setText(context.getString(
                    com.raf.zuhoo.R.string.payslip_net_salary_format, payslip.getNetSalary()));

            StatusBadgeView.bind(binding.itemStatusBadge,
                    PayrollStatusBadge.colorFor(context, payslip.getStatus()),
                    PayrollStatusBadge.labelFor(context, payslip.getStatus()));

            binding.itemBtnDownload.setOnClickListener(v -> listener.onDownload(payslip));
        }
    }
}
