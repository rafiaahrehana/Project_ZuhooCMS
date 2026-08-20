package com.raf.zuhoo.ui.noticeboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.HolidayResponse;
import com.raf.zuhoo.databinding.ItemHolidayBinding;

import java.util.List;

public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.ViewHolder> {

    private final List<HolidayResponse> holidays;

    public HolidayAdapter(List<HolidayResponse> holidays) {
        this.holidays = holidays;
    }

    public void submitList(List<HolidayResponse> newHolidays) {
        holidays.clear();
        holidays.addAll(newHolidays);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHolidayBinding binding = ItemHolidayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(holidays.get(position));
    }

    @Override
    public int getItemCount() {
        return holidays.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemHolidayBinding binding;

        ViewHolder(ItemHolidayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HolidayResponse holiday) {
            binding.holidayDate.setText(holiday.getHolidayDate());
            binding.holidayName.setText(holiday.getName());
        }
    }
}
