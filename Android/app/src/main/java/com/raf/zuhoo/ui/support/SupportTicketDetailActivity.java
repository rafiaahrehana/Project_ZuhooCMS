package com.raf.zuhoo.ui.support;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.data.model.TicketStatus;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.databinding.ActivitySupportTicketDetailBinding;
import com.raf.zuhoo.ui.common.AttachmentPicker;
import com.raf.zuhoo.ui.common.StatusBadgeView;
import com.raf.zuhoo.ui.common.UiErrors;

import java.util.ArrayList;

public class SupportTicketDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "extra_ticket_id";

    private ActivitySupportTicketDetailBinding binding;
    private SupportTicketDetailViewModel viewModel;
    private SupportMessageAdapter messageAdapter;
    private AttachmentPicker attachmentPicker;

    private long ticketId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySupportTicketDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ticketId = getIntent().getLongExtra(EXTRA_TICKET_ID, -1);

        if (ticketId < 0) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(SupportTicketDetailViewModel.class);

        messageAdapter = new SupportMessageAdapter(new ArrayList<>());
        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerView.setAdapter(messageAdapter);

        attachmentPicker = new AttachmentPicker(this, binding.attachmentChip);

        binding.btnAttach.setOnClickListener(v -> attachmentPicker.pick());
        binding.btnSendMessage.setOnClickListener(v -> sendMessage());
        binding.btnRateSupport.setOnClickListener(v -> promptSatisfaction());

        observeViewModel();

        viewModel.start(ticketId);
    }

    private void observeViewModel() {

        viewModel.ticket().observe(this, this::bindDetail);

        viewModel.messages().observe(this, messages -> {
            messageAdapter.submitList(messages);
            binding.messagesRecyclerView.scrollToPosition(messageAdapter.getMessageCount() - 1);
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
            ApiErrors.ApiError error = event.consume();
            if (error != null) {
                UiErrors.show(this, error);
            }
        });

        viewModel.messageSent().observe(this, event -> {
            Boolean sent = event.consume();
            if (sent == null) {
                return;
            }
            binding.btnSendMessage.setEnabled(true);
            if (sent) {
                binding.messageInput.setText("");
                attachmentPicker.clear();
            }
        });

        viewModel.rated().observe(this, rated -> {
            if (rated) {
                binding.btnRateSupport.setVisibility(View.GONE);
            }
        });
    }

    private void bindDetail(SupportTicketResponse ticket) {

        binding.detailTitle.setText(ticket.getTitle());
        StatusBadgeView.bind(binding.detailStatusBadge,
                TicketStatusBadge.colorFor(this, ticket.getStatus()),
                TicketStatusBadge.labelFor(this, ticket.getStatus()));
        binding.detailTicketNumber.setText(ticket.getTicketNumber());
        binding.detailDescription.setText(ticket.getDescription());
        binding.detailPriority.setText(ticket.getPriority());
        binding.detailAssignedTo.setText(getString(R.string.detail_assigned_to,
                ticket.getAssignedToAgentName() != null
                        ? ticket.getAssignedToAgentName() : getString(R.string.detail_unassigned)));

        boolean canRate = TicketStatus.RESOLVED.equals(ticket.getStatus())
                && ticket.getSatisfactionRating() == null
                && !Boolean.TRUE.equals(viewModel.rated().getValue());
        binding.btnRateSupport.setVisibility(canRate ? View.VISIBLE : View.GONE);
    }

    private void sendMessage() {

        String content = binding.messageInput.getText() == null
                ? "" : binding.messageInput.getText().toString().trim();

        // An attachment on its own is a legitimate message — content is only required when
        // there's nothing else to send.
        if (TextUtils.isEmpty(content) && attachmentPicker.url() == null) {
            return;
        }

        binding.btnSendMessage.setEnabled(false);
        viewModel.sendMessage(content, attachmentPicker.url(), attachmentPicker.fileName());
    }

    private void promptSatisfaction() {

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);
        ratingBar.setRating(5);

        EditText feedbackInput = new EditText(this);
        feedbackInput.setHint(getString(R.string.hint_satisfaction_feedback_optional));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(ratingBar);
        container.addView(feedbackInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_satisfaction_title)
                .setView(container)
                .setPositiveButton(R.string.action_submit_satisfaction, (dialog, which) -> {
                    int rating = Math.round(ratingBar.getRating());
                    String feedback = feedbackInput.getText() == null
                            ? "" : feedbackInput.getText().toString().trim();
                    viewModel.submitSatisfaction(rating, feedback);
                })
                .setNegativeButton(R.string.action_no, null)
                .show();
    }
}
