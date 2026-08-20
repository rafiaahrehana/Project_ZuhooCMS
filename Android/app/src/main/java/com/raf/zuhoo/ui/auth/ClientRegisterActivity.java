package com.raf.zuhoo.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.PasswordPolicy;
import com.raf.zuhoo.data.model.request.PublicClientRegisterRequest;
import com.raf.zuhoo.data.model.response.ClientResponse;
import com.raf.zuhoo.data.model.response.CompanyPublicResponse;
import com.raf.zuhoo.data.repository.ClientRepository;
import com.raf.zuhoo.data.repository.CompanyRepository;
import com.raf.zuhoo.databinding.ActivityClientRegisterBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientRegisterActivity extends AppCompatActivity {

    private ActivityClientRegisterBinding binding;

    private CompanyRepository companyRepository;
    private ClientRepository clientRepository;

    private final List<CompanyPublicResponse> companies = new ArrayList<>();
    // -1 until loadCompanies() populates the dropdown and defaults it to the first entry — mirrors
    // the old Spinner's implicit position-0 selection once its adapter had data.
    private int companyIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityClientRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        companyRepository = new CompanyRepository(this);
        clientRepository = new ClientRepository(this);

        loadCompanies();

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void loadCompanies() {

        companyRepository.getPublicCompanies(new Callback<List<CompanyPublicResponse>>() {

            @Override
            public void onResponse(Call<List<CompanyPublicResponse>> call,
                                   Response<List<CompanyPublicResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ClientRegisterActivity.this,
                            R.string.error_companies_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                companies.clear();
                companies.addAll(response.body());

                if (companies.isEmpty()) {
                    return;
                }

                List<String> labels = new ArrayList<>();
                for (CompanyPublicResponse company : companies) {
                    labels.add(company.getCompanyName());
                }

                binding.companyDropdown.setAdapter(new ArrayAdapter<>(
                        ClientRegisterActivity.this,
                        android.R.layout.simple_list_item_1, labels));
                // Non-editable dropdown defaults to the first company, same as the old Spinner's
                // implicit position-0 selection — attemptRegister() reads companyIndex, never the
                // field's text.
                binding.companyDropdown.setText(labels.get(0), false);
                companyIndex = 0;
                binding.companyDropdown.setOnItemClickListener((parent, view, position, id) -> companyIndex = position);
            }

            @Override
            public void onFailure(Call<List<CompanyPublicResponse>> call, Throwable t) {
                Toast.makeText(ClientRegisterActivity.this,
                        R.string.error_companies_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptRegister() {

        String firstName = textOf(binding.firstNameEditText);
        String lastName = textOf(binding.lastNameEditText);
        String email = textOf(binding.emailEditText);
        String password = textOf(binding.passwordEditText);
        String phone = textOf(binding.phoneEditText);
        String clientCompanyName = textOf(binding.clientCompanyNameEditText);
        String industry = textOf(binding.industryEditText);
        String website = textOf(binding.websiteEditText);

        binding.firstNameInputLayout.setError(null);
        binding.lastNameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(firstName)) {
            binding.firstNameInputLayout.setError(getString(R.string.error_first_name_required));
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            binding.lastNameInputLayout.setError(getString(R.string.error_last_name_required));
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }

        if (!PasswordPolicy.isValid(password)) {
            binding.passwordInputLayout.setError(getString(R.string.error_password_weak));
            return;
        }

        if (companyIndex < 0 || companyIndex >= companies.size()) {
            Toast.makeText(this, R.string.error_company_required, Toast.LENGTH_LONG).show();
            return;
        }

        Long companyId = companies.get(companyIndex).getId();

        setLoading(true);

        PublicClientRegisterRequest request = new PublicClientRegisterRequest(
                firstName, lastName, email, password,
                nullIfEmpty(phone), companyId,
                nullIfEmpty(clientCompanyName), nullIfEmpty(industry), nullIfEmpty(website));

        clientRepository.registerPublic(request, new Callback<ClientResponse>() {

            @Override
            public void onResponse(Call<ClientResponse> call, Response<ClientResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(ClientRegisterActivity.this, response, getString(R.string.error_login_failed));
                    return;
                }

                Toast.makeText(ClientRegisterActivity.this,
                        R.string.registration_success_client, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Call<ClientResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ClientRegisterActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private String nullIfEmpty(String value) {
        return TextUtils.isEmpty(value) ? null : value;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }
}
