package com.raf.zuhoo.ui.leave;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.repository.LeaveRequestRepository;
import com.raf.zuhoo.databinding.ActivityLeaveApprovalListBinding;
import com.raf.zuhoo.ui.common.CacheStamp;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Leave Approvals" — pending leave requests across the company, for review. Reachable from the
 * Dashboard by any staff member (same tier as the existing staff service-request buttons); the
 * server's LEAVE_VIEW/LEAVE_APPROVE permissions are the real gate, this screen just shows the
 * entry point (the app has no client-side permission model — see StaffServiceRequestListActivity).
 */
public class LeaveApprovalListActivity extends AppCompatActivity {

    private ActivityLeaveApprovalListBinding binding;
    private LeaveApprovalListViewModel viewModel;
    private LeaveRequestRepository repository;
    private LeaveRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLeaveApprovalListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LeaveApprovalListViewModel.class);
        repository = new LeaveRequestRepository(this);

        // showEmployeeName=true, no cancel listener — a tap opens the approve/reject dialog.
        adapter = new LeaveRequestAdapter(new ArrayList<>(), true, this::openReviewDialog, null);
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsRecyclerView.setAdapter(adapter);

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.start();
    }

    private void observeViewModel() {

        viewModel.items().observe(this, requests -> {
            adapter.submitList(requests);
            if (requests.isEmpty()) {
                binding.stateView.showEmpty(R.drawable.ic_inbox,
                        R.string.empty_leave_approvals, R.string.empty_leave_approvals_subtitle);
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
    }

    private void openReviewDialog(LeaveRequestResponse request) {

        String message = request.getEmployeeName() + "\n"
                + getString(R.string.leave_date_range_format,
                        request.getStartDate(), request.getEndDate(), request.getTotalDays())
                + (TextUtils.isEmpty(request.getReason()) ? "" : "\n\n" + request.getReason());

        new MaterialAlertDialogBuilder(this)
                .setTitle(LeaveTypeLabels.labelFor(this, request.getLeaveType()))
                .setMessage(message)
                .setPositiveButton(R.string.action_approve, (dialog, which) -> approve(request))
                .setNegativeButton(R.string.action_reject, (dialog, which) -> promptRejectReason(request))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void promptRejectReason(LeaveRequestResponse request) {

        EditText reasonInput = new EditText(this);
        reasonInput.setHint(getString(R.string.hint_rejection_reason));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reject_leave_title)
                .setView(reasonInput)
                .setPositiveButton(R.string.action_reject, (dialog, which) -> {

                    String reason = reasonInput.getText() == null
                            ? "" : reasonInput.getText().toString().trim();

                    if (TextUtils.isEmpty(reason)) {
                        Toast.makeText(this, R.string.error_rejection_reason_required, Toast.LENGTH_LONG).show();
                        return;
                    }

                    reject(request, reason);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void approve(LeaveRequestResponse request) {

        repository.approveLeaveRequest(request.getId(), new Callback<LeaveRequestResponse>() {

            @Override
            public void onResponse(Call<LeaveRequestResponse> call, Response<LeaveRequestResponse> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(LeaveApprovalListActivity.this, response,
                            getString(R.string.error_leave_review_failed));
                    return;
                }

                Toast.makeText(LeaveApprovalListActivity.this, R.string.leave_approved, Toast.LENGTH_SHORT).show();
                viewModel.refresh();
            }

            @Override
            public void onFailure(Call<LeaveRequestResponse> call, Throwable t) {
                Toast.makeText(LeaveApprovalListActivity.this, R.string.error_leave_review_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reject(LeaveRequestResponse request, String reason) {

        repository.rejectLeaveRequest(request.getId(), reason, new Callback<LeaveRequestResponse>() {

            @Override
            public void onResponse(Call<LeaveRequestResponse> call, Response<LeaveRequestResponse> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(LeaveApprovalListActivity.this, response,
                            getString(R.string.error_leave_review_failed));
                    return;
                }

                Toast.makeText(LeaveApprovalListActivity.this, R.string.leave_rejected, Toast.LENGTH_SHORT).show();
                viewModel.refresh();
            }

            @Override
            public void onFailure(Call<LeaveRequestResponse> call, Throwable t) {
                Toast.makeText(LeaveApprovalListActivity.this, R.string.error_leave_review_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}
