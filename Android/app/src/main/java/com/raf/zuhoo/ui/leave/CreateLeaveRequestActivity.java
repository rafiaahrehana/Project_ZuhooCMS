package com.raf.zuhoo.ui.leave;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.LeaveType;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.repository.LeaveRequestRepository;
import com.raf.zuhoo.databinding.ActivityCreateLeaveRequestBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateLeaveRequestActivity extends AppCompatActivity {

    private ActivityCreateLeaveRequestBinding binding;
    private LeaveRequestRepository repository;

    private String startDate;
    private String endDate;
    private int leaveTypeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateLeaveRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new LeaveRequestRepository(this);

        String[] leaveTypeLabels = LeaveTypeLabels.allLabels(this);
        binding.leaveTypeDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, leaveTypeLabels));
        // Non-editable dropdown defaults to the first option, same as Spinner's implicit
        // position-0 selection — attemptSubmit() reads leaveTypeIndex, never the field's text.
        binding.leaveTypeDropdown.setText(leaveTypeLabels[0], false);
        binding.leaveTypeDropdown.setOnItemClickListener((parent, view, position, id) -> leaveTypeIndex = position);

        binding.startDateText.setOnClickListener(v -> pickDate(binding.startDateText, picked -> startDate = picked));
        binding.endDateText.setOnClickListener(v -> pickDate(binding.endDateText, picked -> endDate = picked));

        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());
    }

    private interface OnDatePicked {
        void onPicked(String isoDate);
    }

    // Same ISO-8601-via-DatePickerDialog pattern as DynamicFormRenderer.datePicker() — the only
    // date-picker precedent in the app, reused here instead of introducing a second one.
    private void pickDate(TextView target, OnDatePicked onPicked) {

        Calendar now = Calendar.getInstance();

        new DatePickerDialog(this, (picker, year, month, day) -> {
            String iso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            target.setText(iso);
            onPicked.onPicked(iso);
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void attemptSubmit() {

        if (startDate == null) {
            Toast.makeText(this, R.string.error_start_date_required, Toast.LENGTH_LONG).show();
            return;
        }
        if (endDate == null) {
            Toast.makeText(this, R.string.error_end_date_required, Toast.LENGTH_LONG).show();
            return;
        }
        if (endDate.compareTo(startDate) < 0) {
            Toast.makeText(this, R.string.error_end_date_before_start, Toast.LENGTH_LONG).show();
            return;
        }

        String leaveType = LeaveType.VALUES[leaveTypeIndex];

        String reason = binding.reasonEditText.getText() == null
                ? null : binding.reasonEditText.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            reason = null;
        }

        setLoading(true);

        repository.createLeaveRequest(leaveType, startDate, endDate, reason,
                new Callback<LeaveRequestResponse>() {

            @Override
            public void onResponse(Call<LeaveRequestResponse> call, Response<LeaveRequestResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CreateLeaveRequestActivity.this, response,
                            getString(R.string.error_leave_request_failed));
                    return;
                }

                Toast.makeText(CreateLeaveRequestActivity.this,
                        R.string.leave_request_submitted, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<LeaveRequestResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateLeaveRequestActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
