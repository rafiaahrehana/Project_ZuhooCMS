package com.raf.zuhoo.ui.support;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.response.SupportCategoryResponse;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.data.repository.SupportTicketRepository;
import com.raf.zuhoo.databinding.ActivityCreateSupportTicketBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSupportTicketActivity extends AppCompatActivity {

    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};

    private ActivityCreateSupportTicketBinding binding;
    private SupportTicketRepository supportTicketRepository;

    private final List<SupportCategoryResponse> categories = new ArrayList<>();
    private int categoryIndex = 0;
    // Defaults to MEDIUM (index 1), same as the old Spinner's setSelection(1).
    private int priorityIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateSupportTicketBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        supportTicketRepository = new SupportTicketRepository(this);

        binding.priorityDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, PRIORITIES));
        binding.priorityDropdown.setText(PRIORITIES[priorityIndex], false);
        binding.priorityDropdown.setOnItemClickListener((parent, view, position, id) -> priorityIndex = position);

        loadCategories();

        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());
    }

    private void loadCategories() {

        supportTicketRepository.getSupportCategories(new Callback<List<SupportCategoryResponse>>() {

            @Override
            public void onResponse(Call<List<SupportCategoryResponse>> call,
                                   Response<List<SupportCategoryResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                categories.clear();
                categories.addAll(response.body());

                List<String> labels = new ArrayList<>();
                labels.add(getString(R.string.hint_ticket_category));
                for (SupportCategoryResponse category : categories) {
                    labels.add(category.getCategoryName());
                }

                binding.categoryDropdown.setAdapter(new ArrayAdapter<>(
                        CreateSupportTicketActivity.this,
                        android.R.layout.simple_list_item_1, labels));
                // Non-editable dropdown defaults to the first ("no category") option, same as the
                // old Spinner's implicit position-0 selection — attemptSubmit() reads
                // categoryIndex, never the field's text.
                binding.categoryDropdown.setText(labels.get(0), false);
                binding.categoryDropdown.setOnItemClickListener((parent, view, position, id) -> categoryIndex = position);
            }

            @Override
            public void onFailure(Call<List<SupportCategoryResponse>> call, Throwable t) {
                // category is optional — silently proceed without a picker
            }
        });
    }

    private void attemptSubmit() {

        String title = binding.titleEditText.getText() == null
                ? "" : binding.titleEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText() == null
                ? "" : binding.descriptionEditText.getText().toString().trim();

        binding.titleInputLayout.setError(null);
        binding.descriptionInputLayout.setError(null);

        if (TextUtils.isEmpty(title)) {
            binding.titleInputLayout.setError(getString(R.string.error_ticket_title_required));
            return;
        }

        if (TextUtils.isEmpty(description)) {
            binding.descriptionInputLayout.setError(getString(R.string.error_ticket_description_required));
            return;
        }

        Long categoryId = (categoryIndex > 0 && categoryIndex - 1 < categories.size())
                ? categories.get(categoryIndex - 1).getId() : null;

        String priority = PRIORITIES[priorityIndex];

        setLoading(true);

        supportTicketRepository.createTicket(title, description, categoryId, priority,
                new Callback<SupportTicketResponse>() {

            @Override
            public void onResponse(Call<SupportTicketResponse> call, Response<SupportTicketResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CreateSupportTicketActivity.this, response, getString(R.string.error_ticket_create_failed));
                    return;
                }

                Toast.makeText(CreateSupportTicketActivity.this,
                        R.string.ticket_created, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<SupportTicketResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateSupportTicketActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
