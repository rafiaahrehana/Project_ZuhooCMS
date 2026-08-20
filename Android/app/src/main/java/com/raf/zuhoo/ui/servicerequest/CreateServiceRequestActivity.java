package com.raf.zuhoo.ui.servicerequest;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.ServiceRequestPriority;
import com.raf.zuhoo.data.model.request.CreateServiceRequestRequest;
import com.raf.zuhoo.data.model.response.CompanyServiceResponse;
import com.raf.zuhoo.data.model.response.ServiceFormField;
import com.raf.zuhoo.data.model.response.ServiceRequestDetail;
import com.raf.zuhoo.data.repository.CatalogRepository;
import com.raf.zuhoo.data.repository.ServiceRequestRepository;
import com.raf.zuhoo.databinding.ActivityCreateServiceRequestBinding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateServiceRequestActivity extends AppCompatActivity {

    public static final String EXTRA_PRESELECTED_SERVICE_ID = "extra_preselected_service_id";

    // Wire constants sent to the API. Note NORMAL, not MEDIUM — see ServiceRequestPriority.
    private static final String[] PRIORITY_VALUES = {
            null,
            ServiceRequestPriority.LOW,
            ServiceRequestPriority.NORMAL,
            ServiceRequestPriority.HIGH,
            ServiceRequestPriority.URGENT
    };

    // Parallel to PRIORITY_VALUES; resolved from resources so the spinner is localized while
    // what goes over the wire stays English.
    private static final int[] PRIORITY_LABEL_RES = {
            R.string.priority_default,
            R.string.priority_low,
            R.string.priority_normal,
            R.string.priority_high,
            R.string.priority_urgent
    };

    private ActivityCreateServiceRequestBinding binding;
    private CatalogRepository catalogRepository;
    private ServiceRequestRepository serviceRequestRepository;
    private DynamicFormRenderer formRenderer;

    private final List<CompanyServiceResponse> services = new ArrayList<>();
    private long preselectedServiceId;

    // Replace Spinner.getSelectedItemPosition() reads now that the pickers are non-editable
    // AutoCompleteTextView dropdowns — see CreateLeaveRequestActivity's leaveTypeIndex for the
    // same pattern. -1 means "no service loaded/selected yet".
    private int selectedServiceIndex = -1;
    private int priorityIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCreateServiceRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        catalogRepository = new CatalogRepository(this);
        serviceRequestRepository = new ServiceRequestRepository(this);

        preselectedServiceId = getIntent().getLongExtra(EXTRA_PRESELECTED_SERVICE_ID, -1);

        String[] priorityLabels = new String[PRIORITY_LABEL_RES.length];
        for (int i = 0; i < PRIORITY_LABEL_RES.length; i++) {
            priorityLabels[i] = getString(PRIORITY_LABEL_RES[i]);
        }

        binding.priorityDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, priorityLabels));
        // Non-editable dropdown defaults to the first option, same as Spinner's implicit
        // position-0 selection — attemptSubmit() reads priorityIndex, never the field's text.
        binding.priorityDropdown.setText(priorityLabels[0], false);
        binding.priorityDropdown.setOnItemClickListener((parent, view, position, id) -> priorityIndex = position);

        formRenderer = new DynamicFormRenderer(this, binding.dynamicFieldsContainer);

        // Each service defines its own custom fields, so the form has to be rebuilt whenever the
        // selection changes — not just once on open.
        binding.serviceDropdown.setOnItemClickListener((parent, view, position, id) -> selectServiceAt(position));

        loadServices();

        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());
    }

    private void selectServiceAt(int position) {

        if (position < 0 || position >= services.size()) {
            return;
        }

        selectedServiceIndex = position;
        binding.serviceDropdown.setText(services.get(position).toString(), false);
        loadFormFields(services.get(position).getId());
    }

    private void loadFormFields(Long serviceId) {

        if (serviceId == null) {
            formRenderer.render(null);
            return;
        }

        catalogRepository.getServiceFormFields(serviceId, new Callback<List<ServiceFormField>>() {

            @Override
            public void onResponse(Call<List<ServiceFormField>> call,
                                   Response<List<ServiceFormField>> response) {

                formRenderer.render(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<ServiceFormField>> call, Throwable t) {
                // A service with no reachable field definitions still submits fine — formData is
                // optional. Better a plain form than a blocked one.
                formRenderer.render(null);
            }
        });
    }

    private void loadServices() {

        catalogRepository.getActiveServices(new Callback<List<CompanyServiceResponse>>() {

            @Override
            public void onResponse(Call<List<CompanyServiceResponse>> call,
                                   Response<List<CompanyServiceResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CreateServiceRequestActivity.this,
                            R.string.error_services_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                services.clear();
                services.addAll(response.body());

                binding.serviceDropdown.setAdapter(new ArrayAdapter<>(
                        CreateServiceRequestActivity.this,
                        android.R.layout.simple_list_item_1, services));

                if (services.isEmpty()) {
                    formRenderer.render(null);
                    return;
                }

                // The dropdown never auto-fires an item-click the way Spinner auto-selected
                // position 0 (and re-fired on setSelection()), so the initial/preselected
                // selection — and the custom-fields load it drives — has to be triggered here.
                int initialIndex = 0;
                if (preselectedServiceId >= 0) {
                    for (int i = 0; i < services.size(); i++) {
                        if (services.get(i).getId() != null && services.get(i).getId() == preselectedServiceId) {
                            initialIndex = i;
                            break;
                        }
                    }
                }
                selectServiceAt(initialIndex);
            }

            @Override
            public void onFailure(Call<List<CompanyServiceResponse>> call, Throwable t) {
                Toast.makeText(CreateServiceRequestActivity.this,
                        R.string.error_services_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptSubmit() {

        String title = binding.titleEditText.getText() == null
                ? "" : binding.titleEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText() == null
                ? "" : binding.descriptionEditText.getText().toString().trim();
        String priceText = binding.agreedPriceEditText.getText() == null
                ? "" : binding.agreedPriceEditText.getText().toString().trim();

        binding.titleInputLayout.setError(null);

        if (TextUtils.isEmpty(title)) {
            binding.titleInputLayout.setError(getString(R.string.error_title_required));
            return;
        }

        if (selectedServiceIndex < 0 || selectedServiceIndex >= services.size()) {
            Toast.makeText(this, R.string.error_service_required, Toast.LENGTH_LONG).show();
            return;
        }

        Long hubServiceId = services.get(selectedServiceIndex).getId();

        String priority = PRIORITY_VALUES[priorityIndex];

        BigDecimal agreedPrice = null;
        if (!TextUtils.isEmpty(priceText)) {
            try {
                agreedPrice = new BigDecimal(priceText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_price_invalid, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Mirror the backend's required-field check so the user is told which field is missing
        // rather than getting a generic 400 after submitting.
        String missingField = formRenderer.firstMissingRequiredLabel();
        if (missingField != null) {
            Toast.makeText(this, getString(R.string.error_form_field_required, missingField),
                    Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        CreateServiceRequestRequest request = new CreateServiceRequestRequest(
                title, TextUtils.isEmpty(description) ? null : description,
                hubServiceId, priority, agreedPrice, formRenderer.answers());

        serviceRequestRepository.createServiceRequest(request, new Callback<ServiceRequestDetail>() {

            @Override
            public void onResponse(Call<ServiceRequestDetail> call, Response<ServiceRequestDetail> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CreateServiceRequestActivity.this, response, getString(R.string.error_request_create_failed));
                    return;
                }

                Toast.makeText(CreateServiceRequestActivity.this,
                        R.string.request_created, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<ServiceRequestDetail> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateServiceRequestActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
