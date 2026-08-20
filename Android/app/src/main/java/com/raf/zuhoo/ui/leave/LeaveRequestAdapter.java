package com.raf.zuhoo.ui.leave;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.LeaveRequestStatus;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.databinding.ItemLeaveRequestBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;

import java.util.List;

// Shared by "My Leave" (own requests, cancel button on pending rows) and "Leave Approvals"
// (other employees' pending requests, tap opens an approve/reject dialog instead) — the two
// screens differ only in whether the employee name shows and what a tap/cancel does, so one
// adapter with a mode flag replaces two near-duplicate ones.
public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(LeaveRequestResponse request);
    }

    public interface OnCancelClickListener {
        void onCancel(LeaveRequestResponse request);
    }

    private final List<LeaveRequestResponse> requests;
    private final boolean showEmployeeName;
    private final OnItemClickListener clickListener;
    private final OnCancelClickListener cancelListener;

    /** cancelListener may be null when this list has no cancel action (the approvals screen). */
    public LeaveRequestAdapter(List<LeaveRequestResponse> requests, boolean showEmployeeName,
                               OnItemClickListener clickListener, OnCancelClickListener cancelListener) {
        this.requests = requests;
        this.showEmployeeName = showEmployeeName;
        this.clickListener = clickListener;
        this.cancelListener = cancelListener;
    }

    public void submitList(List<LeaveRequestResponse> newRequests) {
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeaveRequestBinding binding = ItemLeaveRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position), showEmployeeName, clickListener, cancelListener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLeaveRequestBinding binding;

        ViewHolder(ItemLeaveRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LeaveRequestResponse request, boolean showEmployeeName,
                 OnItemClickListener clickListener, OnCancelClickListener cancelListener) {

            android.content.Context context = binding.getRoot().getContext();

            binding.itemLeaveType.setText(LeaveTypeLabels.labelFor(context, request.getLeaveType()));
            binding.itemDateRange.setText(context.getString(
                    com.raf.zuhoo.R.string.leave_date_range_format,
                    request.getStartDate(), request.getEndDate(), request.getTotalDays()));
            binding.itemReason.setText(request.getReason());
            binding.itemReason.setVisibility(
                    request.getReason() == null || request.getReason().isEmpty() ? View.GONE : View.VISIBLE);

            StatusBadgeView.bind(binding.itemStatusBadge,
                    LeaveRequestStatusBadge.colorFor(context, request.getStatus()),
                    LeaveRequestStatusBadge.labelFor(context, request.getStatus()));

            binding.itemEmployeeName.setVisibility(showEmployeeName ? View.VISIBLE : View.GONE);
            if (showEmployeeName) {
                binding.itemEmployeeName.setText(request.getEmployeeName());
            }

            boolean canCancel = cancelListener != null && LeaveRequestStatus.PENDING.equals(request.getStatus());
            binding.itemBtnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            if (canCancel) {
                binding.itemBtnCancel.setOnClickListener(v -> cancelListener.onCancel(request));
            }

            binding.getRoot().setOnClickListener(v -> clickListener.onClick(request));
        }
    }
}
