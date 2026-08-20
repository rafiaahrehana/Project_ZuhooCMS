package com.raf.zuhoo.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.PasswordPolicy;
import com.raf.zuhoo.data.model.request.RegisterRequest;
import com.raf.zuhoo.data.model.response.UserResponse;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.databinding.ActivityCompanyRegisterBinding;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompanyOwnerRegisterActivity extends AppCompatActivity {

    private static final Pattern SUBDOMAIN_PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$");

    private ActivityCompanyRegisterBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompanyRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {

        String firstName = textOf(binding.firstNameEditText);
        String lastName = textOf(binding.lastNameEditText);
        String email = textOf(binding.emailEditText);
        String password = textOf(binding.passwordEditText);
        String companyName = textOf(binding.companyNameEditText);
        String subdomain = textOf(binding.subdomainEditText);
        String companyEmail = textOf(binding.companyEmailEditText);
        String companyPhone = textOf(binding.companyPhoneEditText);

        binding.firstNameInputLayout.setError(null);
        binding.lastNameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);
        binding.companyNameInputLayout.setError(null);
        binding.subdomainInputLayout.setError(null);

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

        if (TextUtils.isEmpty(companyName)) {
            binding.companyNameInputLayout.setError(getString(R.string.error_company_name_required));
            return;
        }

        if (TextUtils.isEmpty(subdomain)) {
            binding.subdomainInputLayout.setError(getString(R.string.error_subdomain_required));
            return;
        }

        if (!SUBDOMAIN_PATTERN.matcher(subdomain).matches()) {
            binding.subdomainInputLayout.setError(getString(R.string.error_subdomain_invalid));
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(
                firstName, lastName, email, password, companyName, subdomain,
                nullIfEmpty(companyEmail), nullIfEmpty(companyPhone));

        authRepository.register(request, new Callback<UserResponse>() {

            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CompanyOwnerRegisterActivity.this, response, getString(R.string.error_login_failed));
                    return;
                }

                Toast.makeText(CompanyOwnerRegisterActivity.this,
                        R.string.registration_success_company, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CompanyOwnerRegisterActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
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
