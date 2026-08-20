package com.raf.zuhoo.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.ExpenseCategory;
import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.data.repository.ExpenseRepository;
import com.raf.zuhoo.databinding.ActivityCreateExpenseBinding;
import com.raf.zuhoo.ui.common.AttachmentPicker;
import com.raf.zuhoo.ui.common.UiErrors;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateExpenseActivity extends AppCompatActivity {

    private ActivityCreateExpenseBinding binding;
    private ExpenseRepository repository;
    private AttachmentPicker attachmentPicker;

    private String expenseDate;
    private int categoryIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ExpenseRepository(this);
        attachmentPicker = new AttachmentPicker(this, binding.attachmentChip);

        String[] categoryLabels = ExpenseCategoryLabels.allLabels(this);
        binding.categoryDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, categoryLabels));
        // Non-editable dropdown defaults to the first option, same as Spinner's implicit
        // position-0 selection — attemptSubmit() reads categoryIndex, never the field's text.
        binding.categoryDropdown.setText(categoryLabels[0], false);
        binding.categoryDropdown.setOnItemClickListener((parent, view, position, id) -> categoryIndex = position);

        binding.expenseDateText.setOnClickListener(v -> pickDate());
        binding.btnAttach.setOnClickListener(v -> attachmentPicker.pick());
        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());
    }

    private void pickDate() {

        Calendar now = Calendar.getInstance();

        new DatePickerDialog(this, (picker, year, month, day) -> {
            expenseDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            binding.expenseDateText.setText(expenseDate);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void attemptSubmit() {

        String description = binding.descriptionEditText.getText() == null
                ? "" : binding.descriptionEditText.getText().toString().trim();
        String amountText = binding.amountEditText.getText() == null
                ? "" : binding.amountEditText.getText().toString().trim();

        binding.descriptionInputLayout.setError(null);
        binding.amountInputLayout.setError(null);

        if (TextUtils.isEmpty(description)) {
            binding.descriptionInputLayout.setError(getString(R.string.error_description_required));
            return;
        }

        BigDecimal amount;
        if (TextUtils.isEmpty(amountText)) {
            binding.amountInputLayout.setError(getString(R.string.error_amount_required));
            return;
        }
        try {
            amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                binding.amountInputLayout.setError(getString(R.string.error_price_invalid));
                return;
            }
        } catch (NumberFormatException e) {
            binding.amountInputLayout.setError(getString(R.string.error_price_invalid));
            return;
        }

        if (expenseDate == null) {
            Toast.makeText(this, R.string.error_expense_date_required, Toast.LENGTH_LONG).show();
            return;
        }

        String category = ExpenseCategory.VALUES[categoryIndex];
        String vendorName = binding.vendorEditText.getText() == null
                ? null : binding.vendorEditText.getText().toString().trim();

        setLoading(true);

        repository.createExpense(description, amount, category,
                TextUtils.isEmpty(vendorName) ? null : vendorName, expenseDate, attachmentPicker.url(),
                new Callback<ExpenseResponse>() {

            @Override
            public void onResponse(Call<ExpenseResponse> call, Response<ExpenseResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CreateExpenseActivity.this, response,
                            getString(R.string.error_expense_submit_failed));
                    return;
                }

                Toast.makeText(CreateExpenseActivity.this, R.string.expense_submitted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<ExpenseResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateExpenseActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
