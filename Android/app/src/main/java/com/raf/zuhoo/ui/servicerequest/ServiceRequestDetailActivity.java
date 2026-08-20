package com.raf.zuhoo.ui.servicerequest;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.ServiceRequestStatus;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.RequestStatusHistory;
import com.raf.zuhoo.data.model.response.ServiceRequestDetail;
import com.raf.zuhoo.databinding.ActivityServiceRequestDetailBinding;
import com.raf.zuhoo.ui.common.AttachmentPicker;
import com.raf.zuhoo.ui.common.StatusBadgeView;
import com.raf.zuhoo.ui.common.UiErrors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServiceRequestDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";

    private ActivityServiceRequestDetailBinding binding;
    private ServiceRequestDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private AttachmentPicker attachmentPicker;

    private long requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityServiceRequestDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestId = getIntent().getLongExtra(EXTRA_REQUEST_ID, -1);

        if (requestId < 0) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ServiceRequestDetailViewModel.class);

        commentAdapter = new CommentAdapter(new ArrayList<>());
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(commentAdapter);

        attachmentPicker = new AttachmentPicker(this, binding.attachmentChip);

        binding.btnAttach.setOnClickListener(v -> attachmentPicker.pick());
        binding.btnSendComment.setOnClickListener(v -> sendComment());
        binding.btnCancelRequest.setOnClickListener(v -> confirmCancel());
        binding.btnAcceptQuotation.setOnClickListener(v -> viewModel.acceptQuotation());
        binding.btnRejectQuotation.setOnClickListener(v -> promptRejectReason());
        binding.btnLeaveReview.setOnClickListener(v -> promptReview());
        binding.btnAssignRequest.setOnClickListener(v -> promptAssign());
        binding.btnChangeStatus.setOnClickListener(v -> promptChangeStatus());
        binding.btnSubmitQuotationStaff.setOnClickListener(v -> promptSubmitQuotation());

        observeViewModel();

        viewModel.start(requestId);
    }

    private void observeViewModel() {

        viewModel.detail().observe(this, this::bindDetail);
        viewModel.history().observe(this, this::bindTimeline);

        viewModel.comments().observe(this, comments -> {
            commentAdapter.submitList(comments);
            binding.commentsRecyclerView.scrollToPosition(commentAdapter.getItemCount() - 1);
        });

        viewModel.loading().observe(this, loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.chatLive().observe(this, live ->
                binding.chatStatusIndicator.setText(
                        live ? R.string.chat_status_live : R.string.chat_status_connecting));

        viewModel.message().observe(this, event -> {
            Integer messageRes = event.consume();
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.apiError().observe(this, event -> {
            com.raf.zuhoo.data.api.ApiErrors.ApiError error = event.consume();
            if (error != null) {
                UiErrors.show(this, error);
            }
        });

        viewModel.commentSent().observe(this, event -> {
            Boolean sent = event.consume();
            if (sent == null) {
                return;
            }
            binding.btnSendComment.setEnabled(true);
            if (sent) {
                binding.commentInput.setText("");
                attachmentPicker.clear();
            }
        });
    }

    private void bindDetail(ServiceRequestDetail detail) {

        binding.detailTitle.setText(detail.getTitle());
        StatusBadgeView.bind(binding.detailStatusBadge,
                StatusBadge.colorFor(this, detail.getStatus()),
                StatusBadge.labelFor(this, detail.getStatus()));
        binding.detailHubService.setText(detail.getHubServiceName());
        binding.detailDescription.setText(TextUtils.isEmpty(detail.getDescription())
                ? getString(R.string.detail_no_description) : detail.getDescription());

        binding.detailPrice.setText(detail.getAgreedPrice() != null
                ? detail.getAgreedPrice().toPlainString() : "-");

        binding.detailAssignedTo.setText(getString(R.string.detail_assigned_to,
                detail.getAssignedEmployeeName() != null
                        ? detail.getAssignedEmployeeName() : getString(R.string.detail_unassigned)));

        if (detail.getQuotationAmount() != null) {
            binding.quotationSection.setVisibility(View.VISIBLE);
            binding.quotationAmountText.setText(getString(R.string.detail_quotation_amount,
                    detail.getQuotationCurrency() != null ? detail.getQuotationCurrency() : "",
                    detail.getQuotationAmount().toPlainString()));
            binding.quotationNotesText.setText(detail.getQuotationNotes());
            binding.quotationNotesText.setVisibility(
                    TextUtils.isEmpty(detail.getQuotationNotes()) ? View.GONE : View.VISIBLE);

            boolean quotationPending = "PENDING".equals(detail.getQuotationStatus());
            binding.quotationActions.setVisibility(
                    quotationPending && !viewModel.isStaff() ? View.VISIBLE : View.GONE);
        } else {
            binding.quotationSection.setVisibility(View.GONE);
        }

        if (viewModel.isStaff()) {
            binding.btnCancelRequest.setVisibility(View.GONE);
            binding.btnLeaveReview.setVisibility(View.GONE);
            binding.staffActions.setVisibility(View.VISIBLE);
        } else {
            boolean canCancel = detail.getAssignedEmployeeName() == null
                    && ServiceRequestStatus.isOpen(detail.getStatus());
            binding.btnCancelRequest.setVisibility(canCancel ? View.VISIBLE : View.GONE);

            boolean canReview = ServiceRequestStatus.COMPLETED.equals(detail.getStatus());
            binding.btnLeaveReview.setVisibility(canReview ? View.VISIBLE : View.GONE);
        }
    }

    private void bindTimeline(List<RequestStatusHistory> history) {

        if (history == null || history.isEmpty()) {
            binding.timelineSection.setVisibility(View.GONE);
            return;
        }

        binding.timelineContainer.removeAllViews();

        int rowSpacing = (int) (getResources().getDisplayMetrics().density * 10);

        for (RequestStatusHistory entry : history) {

            TextView row = new TextView(this);
            TextViewCompat.setTextAppearance(row, R.style.TextAppearance_Zuhoo_BodyMedium);
            row.setPadding(0, 0, 0, rowSpacing);

            StringBuilder line = new StringBuilder()
                    .append(entry.getChangedAt())
                    .append("  •  ")
                    .append(StatusBadge.labelFor(this, entry.getNewStatus()));

            if (!TextUtils.isEmpty(entry.getChangedByName())) {
                line.append("  — ").append(entry.getChangedByName());
            }

            if (!TextUtils.isEmpty(entry.getReason())) {
                line.append('\n').append(entry.getReason());
            }

            row.setText(line);
            binding.timelineContainer.addView(row);
        }

        binding.timelineSection.setVisibility(View.VISIBLE);
    }

    private void sendComment() {

        String content = binding.commentInput.getText() == null
                ? "" : binding.commentInput.getText().toString().trim();

        // An attachment on its own is a legitimate message — content is only required when
        // there's nothing else to send.
        if (TextUtils.isEmpty(content) && attachmentPicker.url() == null) {
            return;
        }

        binding.btnSendComment.setEnabled(false);
        viewModel.sendComment(content, attachmentPicker.url());
    }

    private void promptRejectReason() {

        EditText reasonInput = new EditText(this);
        reasonInput.setHint(getString(R.string.hint_reject_reason_optional));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reject_reason_title)
                .setView(reasonInput)
                .setPositiveButton(R.string.action_reject_quotation, (dialog, which) -> {
                    String reason = reasonInput.getText() == null
                            ? null : reasonInput.getText().toString().trim();
                    viewModel.rejectQuotation(TextUtils.isEmpty(reason) ? null : reason);
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void promptReview() {

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setRating(5);

        EditText commentInput = new EditText(this);
        commentInput.setHint(getString(R.string.hint_review_comment_optional));

        LinearLayout container = verticalDialogContainer();
        container.addView(ratingBar);
        container.addView(commentInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_review_title)
                .setView(container)
                .setPositiveButton(R.string.action_submit_review, (dialog, which) -> {
                    int rating = Math.round(ratingBar.getRating());
                    String comment = commentInput.getText() == null
                            ? null : commentInput.getText().toString().trim();
                    viewModel.submitReview(rating, TextUtils.isEmpty(comment) ? null : comment);
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void promptAssign() {

        viewModel.loadEmployees(new Callback<PageResponse<EmployeeResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<EmployeeResponse>> call,
                                   Response<PageResponse<EmployeeResponse>> response) {

                if (!response.isSuccessful() || response.body() == null
                        || response.body().getContent().isEmpty()) {
                    Toast.makeText(ServiceRequestDetailActivity.this,
                            R.string.error_employees_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                showAssignDialog(response.body().getContent());
            }

            @Override
            public void onFailure(Call<PageResponse<EmployeeResponse>> call, Throwable t) {
                Toast.makeText(ServiceRequestDetailActivity.this,
                        R.string.error_employees_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAssignDialog(List<EmployeeResponse> employees) {

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, employees));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_assign_title)
                .setView(spinner)
                .setPositiveButton(R.string.action_assign_request, (dialog, which) -> {
                    int position = spinner.getSelectedItemPosition();
                    if (position >= 0 && position < employees.size()) {
                        viewModel.assign(employees.get(position).getId());
                    }
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void promptChangeStatus() {

        String[] statuses = {
                ServiceRequestStatus.PENDING, ServiceRequestStatus.QUOTATION_PENDING,
                ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.WAITING_CLIENT, ServiceRequestStatus.UNDER_REVIEW,
                ServiceRequestStatus.COMPLETED, ServiceRequestStatus.REJECTED,
                ServiceRequestStatus.CANCELLED
        };

        // Show localized labels but send the wire constants.
        String[] labels = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            labels[i] = StatusBadge.labelFor(this, statuses[i]);
        }

        Spinner statusSpinner = new Spinner(this);
        statusSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));

        EditText reasonInput = new EditText(this);
        reasonInput.setHint(getString(R.string.hint_status_reason_optional));

        LinearLayout container = verticalDialogContainer();
        container.addView(statusSpinner);
        container.addView(reasonInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_change_status_title)
                .setView(container)
                .setPositiveButton(R.string.action_change_status, (dialog, which) -> {
                    String status = statuses[statusSpinner.getSelectedItemPosition()];
                    String reason = reasonInput.getText() == null
                            ? null : reasonInput.getText().toString().trim();
                    viewModel.changeStatus(status, TextUtils.isEmpty(reason) ? null : reason);
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void promptSubmitQuotation() {

        EditText amountInput = new EditText(this);
        amountInput.setHint(getString(R.string.hint_quotation_amount));
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText currencyInput = new EditText(this);
        currencyInput.setHint(getString(R.string.hint_quotation_currency));

        EditText notesInput = new EditText(this);
        notesInput.setHint(getString(R.string.hint_quotation_notes_optional));

        LinearLayout container = verticalDialogContainer();
        container.addView(amountInput);
        container.addView(currencyInput);
        container.addView(notesInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_submit_quotation_title)
                .setView(container)
                .setPositiveButton(R.string.action_submit_quotation, (dialog, which) -> {

                    String amountText = amountInput.getText() == null
                            ? "" : amountInput.getText().toString().trim();

                    if (TextUtils.isEmpty(amountText)) {
                        Toast.makeText(this, R.string.error_quotation_amount_required,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.error_price_invalid, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String currency = currencyInput.getText() == null
                            ? null : currencyInput.getText().toString().trim();
                    String notes = notesInput.getText() == null
                            ? null : notesInput.getText().toString().trim();

                    viewModel.submitQuotation(amount, TextUtils.isEmpty(currency) ? null : currency,
                            TextUtils.isEmpty(notes) ? null : notes);
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(R.string.dialog_cancel_message)
                .setPositiveButton(R.string.action_yes_cancel,
                        (dialog, which) -> viewModel.cancelRequest())
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private LinearLayout verticalDialogContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        return container;
    }
}
