package com.raf.zuhoo.ui.expense;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.ExpenseRepository;
import com.raf.zuhoo.databinding.ActivityExpenseApprovalListBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Expense Approvals" — pending claims across the company. Reachable from the Dashboard by any
 * staff member; EXPENSE_VIEW/EXPENSE_APPROVE are the server's real gate (same accepted
 * convention as LeaveApprovalListActivity — this app has no client-side permission model).
 */
public class ExpenseApprovalListActivity extends AppCompatActivity {

    private ActivityExpenseApprovalListBinding binding;
    private ExpenseRepository repository;
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityExpenseApprovalListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ExpenseRepository(this);

        adapter = new ExpenseAdapter(new ArrayList<>(), true, this::openReviewDialog);
        binding.expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.expensesRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {

        binding.stateView.showLoading();

        repository.getPendingExpenses(new Callback<PageResponse<ExpenseResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<ExpenseResponse>> call,
                                   Response<PageResponse<ExpenseResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    // A refresh here may be replacing already-loaded content, so this can't
                    // safely take over the whole screen the way an empty-state error can.
                    binding.stateView.showContent();
                    UiErrors.show(ExpenseApprovalListActivity.this, response,
                            getString(R.string.error_expense_approvals_load_failed));
                    return;
                }

                java.util.List<ExpenseResponse> expenses = response.body().getContent();
                adapter.submitList(expenses);
                if (expenses.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_receipt,
                            R.string.empty_expense_approvals, R.string.empty_expense_approvals_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ExpenseResponse>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(ExpenseApprovalListActivity.this,
                        R.string.error_expense_approvals_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openReviewDialog(ExpenseResponse expense) {

        String message = expense.getSubmittedByName() + "\n"
                + ExpenseCategoryLabels.labelFor(this, expense.getCategory())
                + " · " + expense.getExpenseDate()
                + "\n" + expense.getCurrency() + " " + expense.getAmount();

        new MaterialAlertDialogBuilder(this)
                .setTitle(expense.getDescription())
                .setMessage(message)
                .setPositiveButton(R.string.action_approve, (dialog, which) -> approve(expense))
                .setNegativeButton(R.string.action_reject, (dialog, which) -> promptRejectReason(expense))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void promptRejectReason(ExpenseResponse expense) {

        EditText reasonInput = new EditText(this);
        reasonInput.setHint(getString(R.string.hint_rejection_reason));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reject_expense_title)
                .setView(reasonInput)
                .setPositiveButton(R.string.action_reject, (dialog, which) -> {

                    String reason = reasonInput.getText() == null
                            ? "" : reasonInput.getText().toString().trim();

                    if (TextUtils.isEmpty(reason)) {
                        Toast.makeText(this, R.string.error_rejection_reason_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    reject(expense, reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void approve(ExpenseResponse expense) {

        // The backend's notes @RequestParam is required with no default — Retrofit drops a null
        // @Query value from the URL entirely, which the server then sees as a missing parameter.
        repository.approveExpense(expense.getId(), "", new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(ExpenseApprovalListActivity.this, response,
                            getString(R.string.error_expense_review_failed));
                    return;
                }

                Toast.makeText(ExpenseApprovalListActivity.this, R.string.expense_approved, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ExpenseApprovalListActivity.this, R.string.error_expense_review_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reject(ExpenseResponse expense, String reason) {

        repository.rejectExpense(expense.getId(), reason, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(ExpenseApprovalListActivity.this, response,
                            getString(R.string.error_expense_review_failed));
                    return;
                }

                Toast.makeText(ExpenseApprovalListActivity.this, R.string.expense_rejected, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ExpenseApprovalListActivity.this, R.string.error_expense_review_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}
