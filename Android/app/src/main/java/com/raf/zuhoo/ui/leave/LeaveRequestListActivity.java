package com.raf.zuhoo.ui.leave;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.repository.LeaveRequestRepository;
import com.raf.zuhoo.databinding.ActivityLeaveRequestListBinding;
import com.raf.zuhoo.ui.common.CacheStamp;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

/**
 * "My Leave" — own leave requests plus a balance summary. Not a bottom-nav destination: reached
 * from a Dashboard button (same convention as CheckInActivity), since it's a focused task screen
 * rather than a persistent tab.
 */
public class LeaveRequestListActivity extends AppCompatActivity {

    private ActivityLeaveRequestListBinding binding;
    private LeaveRequestListViewModel viewModel;
    private LeaveRequestRepository repository;
    private LeaveRequestAdapter adapter;
    private LeaveBalanceAdapter balanceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLeaveRequestListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LeaveRequestListViewModel.class);
        repository = new LeaveRequestRepository(this);

        adapter = new LeaveRequestAdapter(new ArrayList<>(), false, this::openDetailDialog, this::confirmCancel);
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsRecyclerView.setAdapter(adapter);

        balanceAdapter = new LeaveBalanceAdapter(new ArrayList<>());
        binding.balancesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.balancesRecyclerView.setAdapter(balanceAdapter);

        binding.btnNewLeaveRequest.setOnClickListener(v ->
                startActivity(new Intent(this, CreateLeaveRequestActivity.class)));

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-entering after submitting or cancelling a request should show the new state.
        viewModel.start();
    }

    private void observeViewModel() {

        viewModel.items().observe(this, requests -> {
            adapter.submitList(requests);
            if (requests.isEmpty()) {
                binding.stateView.showEmpty(R.drawable.ic_calendar,
                        R.string.empty_leave_requests, R.string.empty_leave_requests_subtitle);
            } else {
                binding.stateView.showContent();
            }
        });

        viewModel.loading().observe(this, loading -> {
            if (loading && adapter.getItemCount() == 0) {
                binding.stateView.showLoading();
            }
        });

        viewModel.showingCached().observe(this, cached ->
                CacheStamp.bind(binding.cacheStamp, cached, viewModel.lastUpdated().getValue()));

        // Only fires when there's nothing cached to fall back on (see CachedListViewModel) — a
        // background refresh failing with content already on screen never reaches here.
        viewModel.error().observe(this, event -> {
            Integer messageRes = event.consume();
            if (messageRes != null) {
                binding.stateView.showError(messageRes, v -> viewModel.refresh());
            }
        });

        viewModel.balances().observe(this, balanceAdapter::submitList);
    }

    private void openDetailDialog(LeaveRequestResponse request) {

        StringBuilder message = new StringBuilder();
        message.append(getString(R.string.leave_date_range_format,
                request.getStartDate(), request.getEndDate(), request.getTotalDays()));
        if (request.getReason() != null && !request.getReason().isEmpty()) {
            message.append("\n\n").append(request.getReason());
        }
        if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
            message.append("\n\n").append(getString(R.string.label_rejection_reason))
                    .append(": ").append(request.getRejectionReason());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(LeaveTypeLabels.labelFor(this, request.getLeaveType()))
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void confirmCancel(LeaveRequestResponse request) {

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cancel_leave_title)
                .setMessage(R.string.dialog_cancel_leave_message)
                .setPositiveButton(R.string.action_cancel_leave, (dialog, which) -> cancel(request))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void cancel(LeaveRequestResponse request) {

        repository.cancelLeaveRequest(request.getId(), new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(LeaveRequestListActivity.this, response,
                            getString(R.string.error_leave_cancel_failed));
                    return;
                }

                Toast.makeText(LeaveRequestListActivity.this, R.string.leave_request_cancelled,
                        Toast.LENGTH_SHORT).show();
                viewModel.refreshAll();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LeaveRequestListActivity.this, R.string.error_leave_cancel_failed,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
