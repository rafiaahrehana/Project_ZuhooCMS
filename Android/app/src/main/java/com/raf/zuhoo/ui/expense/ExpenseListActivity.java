package com.raf.zuhoo.ui.expense;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.ExpenseRepository;
import com.raf.zuhoo.databinding.ActivityExpenseListBinding;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseListActivity extends AppCompatActivity {

    private ActivityExpenseListBinding binding;
    private ExpenseRepository repository;
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityExpenseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ExpenseRepository(this);

        adapter = new ExpenseAdapter(new ArrayList<>(), false, this::openDetailDialog);
        binding.expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.expensesRecyclerView.setAdapter(adapter);

        binding.btnNewExpense.setOnClickListener(v ->
                startActivity(new Intent(this, CreateExpenseActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-entering after submitting a new claim should show it.
        load();
    }

    private void load() {

        binding.stateView.showLoading();

        repository.getMyExpenses(new Callback<PageResponse<ExpenseResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<ExpenseResponse>> call,
                                   Response<PageResponse<ExpenseResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    // A refresh here may be replacing already-loaded content, so this can't
                    // safely take over the whole screen the way an empty-state error can.
                    binding.stateView.showContent();
                    Toast.makeText(ExpenseListActivity.this, R.string.error_expenses_load_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                java.util.List<ExpenseResponse> expenses = response.body().getContent();
                adapter.submitList(expenses);
                if (expenses.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_receipt,
                            R.string.empty_expenses, R.string.empty_expenses_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ExpenseResponse>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(ExpenseListActivity.this, R.string.error_expenses_load_failed,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openDetailDialog(ExpenseResponse expense) {

        StringBuilder message = new StringBuilder();
        message.append(ExpenseCategoryLabels.labelFor(this, expense.getCategory()))
                .append(" · ").append(expense.getExpenseDate())
                .append("\n").append(expense.getCurrency()).append(" ").append(expense.getAmount());

        if (!TextUtils.isEmpty(expense.getVendorName())) {
            message.append("\n\n").append(expense.getVendorName());
        }
        if (!TextUtils.isEmpty(expense.getApprovalNotes())) {
            message.append("\n\n").append(getString(R.string.label_approval_notes))
                    .append(": ").append(expense.getApprovalNotes());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(expense.getDescription())
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
