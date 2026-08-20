package com.raf.zuhoo.ui.directory;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.databinding.ItemDirectoryEmployeeBinding;

import java.util.List;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {

    private final List<EmployeeResponse> employees;

    public DirectoryAdapter(List<EmployeeResponse> employees) {
        this.employees = employees;
    }

    public void submitList(List<EmployeeResponse> newEmployees) {
        employees.clear();
        employees.addAll(newEmployees);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDirectoryEmployeeBinding binding = ItemDirectoryEmployeeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(employees.get(position));
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemDirectoryEmployeeBinding binding;

        ViewHolder(ItemDirectoryEmployeeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EmployeeResponse employee) {

            binding.itemName.setText(employee.getFirstName() + " " + employee.getLastName());

            String jobAndDept = joinNonEmpty(employee.getJobTitle(), employee.getDepartmentName());
            binding.itemJobTitleAndDept.setText(jobAndDept);
            binding.itemJobTitleAndDept.setVisibility(TextUtils.isEmpty(jobAndDept)
                    ? android.view.View.GONE : android.view.View.VISIBLE);

            String contact = joinNonEmpty(employee.getEmail(), employee.getPhone());
            binding.itemContact.setText(contact);
            binding.itemContact.setVisibility(TextUtils.isEmpty(contact)
                    ? android.view.View.GONE : android.view.View.VISIBLE);
        }

        private String joinNonEmpty(String a, String b) {
            if (TextUtils.isEmpty(a)) return b == null ? "" : b;
            if (TextUtils.isEmpty(b)) return a;
            return a + " · " + b;
        }
    }
}
