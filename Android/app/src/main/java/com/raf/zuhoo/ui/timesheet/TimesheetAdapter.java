package com.raf.zuhoo.ui.timesheet;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.TimesheetResponse;
import com.raf.zuhoo.databinding.ItemTimesheetBinding;

import java.util.List;
import java.util.Locale;

public class TimesheetAdapter extends RecyclerView.Adapter<TimesheetAdapter.ViewHolder> {

    private final List<TimesheetResponse> entries;

    public TimesheetAdapter(List<TimesheetResponse> entries) {
        this.entries = entries;
    }

    public void submitList(List<TimesheetResponse> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimesheetBinding binding = ItemTimesheetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemTimesheetBinding binding;

        ViewHolder(ItemTimesheetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TimesheetResponse entry) {
            binding.itemWorkDate.setText(entry.getWorkDate());
            binding.itemHours.setText(String.format(Locale.US, "%.1fh", entry.getHoursWorked()));
            binding.itemProjectName.setText(entry.getProjectName());
            binding.itemStatus.setText(entry.getStatus());
        }
    }
}
