package com.raf.zuhoo.ui.timesheet;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.TimesheetResponse;
import com.raf.zuhoo.data.repository.TimesheetRepository;
import com.raf.zuhoo.databinding.ActivityTimesheetListBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimesheetListActivity extends AppCompatActivity {

    private ActivityTimesheetListBinding binding;
    private TimesheetRepository repository;
    private TimesheetAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTimesheetListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new TimesheetRepository(this);

        adapter = new TimesheetAdapter(new ArrayList<>());
        binding.timesheetsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.timesheetsRecyclerView.setAdapter(adapter);

        binding.btnLogTime.setOnClickListener(v -> promptLogTime());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {

        binding.stateView.showLoading();

        repository.getMyTimesheets(new Callback<PageResponse<TimesheetResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<TimesheetResponse>> call,
                                   Response<PageResponse<TimesheetResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showContent();
                    UiErrors.show(TimesheetListActivity.this, response, getString(R.string.error_timesheet_load_failed));
                    return;
                }

                java.util.List<TimesheetResponse> entries = response.body().getContent();
                adapter.submitList(entries);
                if (entries.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_clock,
                            R.string.empty_timesheet, R.string.empty_timesheet_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<TimesheetResponse>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(TimesheetListActivity.this, R.string.error_timesheet_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void promptLogTime() {

        TextView dateText = new TextView(this);
        dateText.setPadding(0, dp(12), 0, dp(12));
        dateText.setText(R.string.form_field_pick_date);
        dateText.setTextColor(getColor(R.color.brand_primary));

        String[] workDate = new String[1];
        dateText.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (picker, year, month, day) -> {
                workDate[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                dateText.setText(workDate[0]);
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });

        EditText hoursInput = new EditText(this);
        hoursInput.setHint(getString(R.string.hint_hours_worked));
        hoursInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText projectInput = new EditText(this);
        projectInput.setHint(getString(R.string.hint_project_name_optional));

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint(getString(R.string.hint_description_optional));

        LinearLayout container = verticalDialogContainer();
        container.addView(dateText);
        container.addView(hoursInput);
        container.addView(projectInput);
        container.addView(descriptionInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_log_time)
                .setView(container)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {

                    if (workDate[0] == null) {
                        Toast.makeText(this, R.string.error_expense_date_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String hoursText = hoursInput.getText() == null ? "" : hoursInput.getText().toString().trim();
                    double hours;
                    try {
                        hours = Double.parseDouble(hoursText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.error_amount_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String project = projectInput.getText() == null ? null : projectInput.getText().toString().trim();
                    String description = descriptionInput.getText() == null ? null : descriptionInput.getText().toString().trim();

                    logTime(workDate[0], hours, TextUtils.isEmpty(project) ? null : project,
                            TextUtils.isEmpty(description) ? null : description);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void logTime(String workDate, double hours, String project, String description) {

        repository.logTimesheet(workDate, hours, project, description, new Callback<TimesheetResponse>() {

            @Override
            public void onResponse(Call<TimesheetResponse> call, Response<TimesheetResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(TimesheetListActivity.this, response, getString(R.string.error_timesheet_log_failed));
                    return;
                }

                Toast.makeText(TimesheetListActivity.this, R.string.timesheet_logged, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onFailure(Call<TimesheetResponse> call, Throwable t) {
                Toast.makeText(TimesheetListActivity.this, R.string.error_timesheet_log_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private LinearLayout verticalDialogContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        container.setPadding(padding, padding, padding, padding);
        return container;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
