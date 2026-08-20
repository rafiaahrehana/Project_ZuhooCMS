package com.raf.zuhoo.ui.crm;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.LeadStatus;
import com.raf.zuhoo.data.model.response.LeadResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.LeadRepository;
import com.raf.zuhoo.databinding.ActivityLeadListBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "My Leads" — a lightweight glance, not the full CRM: view leads assigned to me and change
 * status. No pipeline board, no activity history, no contacts — those stay web-only.
 */
public class LeadListActivity extends AppCompatActivity {

    private ActivityLeadListBinding binding;
    private LeadRepository repository;
    private LeadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLeadListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new LeadRepository(this);

        adapter = new LeadAdapter(new ArrayList<>(), this::openDetailDialog);
        binding.leadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.leadsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {

        if (adapter.getItemCount() == 0) {
            binding.stateView.showLoading();
        }

        repository.getMyLeads(new Callback<PageResponse<LeadResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<LeadResponse>> call, Response<PageResponse<LeadResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(LeadListActivity.this, response, getString(R.string.error_leads_load_failed));
                    if (adapter.getItemCount() == 0) {
                        binding.stateView.showContent();
                    }
                    return;
                }

                java.util.List<LeadResponse> leads = response.body().getContent();
                adapter.submitList(leads);
                if (leads.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_briefcase,
                            R.string.empty_leads, R.string.empty_leads_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<LeadResponse>> call, Throwable t) {
                Toast.makeText(LeadListActivity.this, R.string.error_leads_load_failed, Toast.LENGTH_LONG).show();
                if (adapter.getItemCount() == 0) {
                    binding.stateView.showContent();
                }
            }
        });
    }

    private void openDetailDialog(LeadResponse lead) {

        StringBuilder message = new StringBuilder();
        if (!TextUtils.isEmpty(lead.getIndustry())) {
            message.append(lead.getIndustry()).append("\n");
        }
        if (!TextUtils.isEmpty(lead.getPhone())) {
            message.append(lead.getPhone()).append("\n");
        }
        if (!TextUtils.isEmpty(lead.getEmail())) {
            message.append(lead.getEmail()).append("\n");
        }
        if (lead.getEstimatedValue() != null) {
            message.append(getString(R.string.label_estimated_value)).append(": ")
                    .append(lead.getEstimatedValue()).append("\n");
        }
        if (!TextUtils.isEmpty(lead.getNotes())) {
            message.append("\n").append(lead.getNotes());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(lead.getContactName())
                .setMessage(message.toString())
                .setPositiveButton(R.string.action_change_status, (dialog, which) -> promptChangeStatus(lead))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void promptChangeStatus(LeadResponse lead) {

        String[] labels = new String[LeadStatus.VALUES.length];
        for (int i = 0; i < LeadStatus.VALUES.length; i++) {
            labels[i] = LeadStatusBadge.labelFor(this, LeadStatus.VALUES[i]);
        }

        Spinner statusSpinner = new Spinner(this);
        statusSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));

        int currentIndex = java.util.Arrays.asList(LeadStatus.VALUES).indexOf(lead.getStatus());
        if (currentIndex >= 0) {
            statusSpinner.setSelection(currentIndex);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_change_status_title)
                .setView(statusSpinner)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String newStatus = LeadStatus.VALUES[statusSpinner.getSelectedItemPosition()];
                    updateStatus(lead, newStatus);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateStatus(LeadResponse lead, String newStatus) {

        repository.updateLeadStatus(lead.getId(), newStatus, new Callback<LeadResponse>() {

            @Override
            public void onResponse(Call<LeadResponse> call, Response<LeadResponse> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(LeadListActivity.this, response, getString(R.string.error_lead_status_update_failed));
                    return;
                }

                Toast.makeText(LeadListActivity.this, R.string.lead_status_updated, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onFailure(Call<LeadResponse> call, Throwable t) {
                Toast.makeText(LeadListActivity.this, R.string.error_lead_status_update_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}
