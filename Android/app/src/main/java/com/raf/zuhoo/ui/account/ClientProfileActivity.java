package com.raf.zuhoo.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.SecureScreen;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.response.ClientResponse;
import com.raf.zuhoo.data.repository.ClientProfileRepository;
import com.raf.zuhoo.databinding.ActivityClientProfileBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientProfileActivity extends AppCompatActivity {

    private ActivityClientProfileBinding binding;
    private ClientProfileRepository clientProfileRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shows money / personal details - keep it out of screenshots and recents.
        SecureScreen.apply(this);

        binding = ActivityClientProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        clientProfileRepository = new ClientProfileRepository(this);

        binding.btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {

        binding.progressBar.setVisibility(View.VISIBLE);

        clientProfileRepository.getMyProfile(new Callback<ClientResponse>() {

            @Override
            public void onResponse(Call<ClientResponse> call, Response<ClientResponse> response) {

                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ClientProfileActivity.this,
                            R.string.error_company_profile_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                bindProfile(response.body());
            }

            @Override
            public void onFailure(Call<ClientResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ClientProfileActivity.this,
                        R.string.error_company_profile_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindProfile(ClientResponse profile) {

        binding.detailStatus.setText(profile.getStatus());
        binding.detailOnboardedAt.setText(profile.getOnboardedAt());
        binding.detailAccountManager.setText(profile.getAccountManagerName() != null
                ? profile.getAccountManagerName() : getString(R.string.detail_unassigned));

        binding.companyNameEditText.setText(profile.getClientCompanyName());
        binding.industryEditText.setText(profile.getIndustry());
        binding.websiteEditText.setText(profile.getWebsite());
        binding.billingAddressEditText.setText(profile.getBillingAddress());
        binding.shippingAddressEditText.setText(profile.getShippingAddress());
    }

    private void saveProfile() {

        String companyName = textOf(binding.companyNameEditText);
        String industry = textOf(binding.industryEditText);
        String website = textOf(binding.websiteEditText);
        String billingAddress = textOf(binding.billingAddressEditText);
        String shippingAddress = textOf(binding.shippingAddressEditText);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        clientProfileRepository.updateMyProfile(
                TextUtils.isEmpty(companyName) ? null : companyName,
                TextUtils.isEmpty(industry) ? null : industry,
                TextUtils.isEmpty(website) ? null : website,
                TextUtils.isEmpty(billingAddress) ? null : billingAddress,
                TextUtils.isEmpty(shippingAddress) ? null : shippingAddress,
                new Callback<ClientResponse>() {

            @Override
            public void onResponse(Call<ClientResponse> call, Response<ClientResponse> response) {

                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(ClientProfileActivity.this, response, getString(R.string.error_company_profile_update_failed));
                    return;
                }

                Toast.makeText(ClientProfileActivity.this,
                        R.string.company_profile_updated, Toast.LENGTH_SHORT).show();
                bindProfile(response.body());
            }

            @Override
            public void onFailure(Call<ClientResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                Toast.makeText(ClientProfileActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
