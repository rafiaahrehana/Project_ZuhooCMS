package com.raf.zuhoo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.model.response.CompanyPublicResponse;
import com.raf.zuhoo.data.model.response.LoginResponse;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.data.repository.CompanyRepository;
import com.raf.zuhoo.databinding.ActivityCompleteGoogleSignupBinding;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.ui.dashboard.DashboardActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shown when a Google account is genuine but no user in this system has that email.
 *
 * The app is multi-tenant, so an account can't be created from a Google token alone — every user
 * belongs to a company. This is where that missing piece is collected, after which the backend
 * creates a CLIENT and returns a normal session.
 */
public class CompleteGoogleSignupActivity extends AppCompatActivity {

    public static final String EXTRA_ID_TOKEN = "extra_id_token";
    public static final String EXTRA_EMAIL = "extra_email";

    private ActivityCompleteGoogleSignupBinding binding;
    private AuthRepository authRepository;
    private CompanyRepository companyRepository;
    private TokenManager tokenManager;

    private final List<CompanyPublicResponse> companies = new ArrayList<>();
    private String idToken;
    // -1 until loadCompanies() populates the dropdown and defaults it to the first entry — mirrors
    // the old Spinner's implicit position-0 selection once its adapter had data.
    private int companyIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompleteGoogleSignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        idToken = getIntent().getStringExtra(EXTRA_ID_TOKEN);
        String email = getIntent().getStringExtra(EXTRA_EMAIL);

        if (TextUtils.isEmpty(idToken)) {
            finish();
            return;
        }

        authRepository = new AuthRepository(this);
        companyRepository = new CompanyRepository(this);
        tokenManager = new TokenManager(this);

        binding.explainerText.setText(getString(R.string.complete_signup_explainer,
                email != null ? email : ""));

        binding.btnFinishSignup.setOnClickListener(v -> finishSignup());

        loadCompanies();
    }

    private void loadCompanies() {

        setLoading(true);

        companyRepository.getPublicCompanies(new Callback<List<CompanyPublicResponse>>() {

            @Override
            public void onResponse(Call<List<CompanyPublicResponse>> call,
                                   Response<List<CompanyPublicResponse>> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CompleteGoogleSignupActivity.this,
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
                        CompleteGoogleSignupActivity.this,
                        android.R.layout.simple_list_item_1, labels));
                // Non-editable dropdown defaults to the first company, same as the old Spinner's
                // implicit position-0 selection — finishSignup() reads companyIndex, never the
                // field's text.
                binding.companyDropdown.setText(labels.get(0), false);
                companyIndex = 0;
                binding.companyDropdown.setOnItemClickListener((parent, view, position, id) -> companyIndex = position);
            }

            @Override
            public void onFailure(Call<List<CompanyPublicResponse>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CompleteGoogleSignupActivity.this,
                        R.string.error_companies_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finishSignup() {

        if (companyIndex < 0 || companyIndex >= companies.size()) {
            Toast.makeText(this, R.string.error_company_required, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        authRepository.googleRegister(
                idToken,
                companies.get(companyIndex).getId(),
                text(binding.phoneEditText.getText()),
                text(binding.clientCompanyEditText.getText()),
                text(binding.industryEditText.getText()),
                null,
                new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                        setLoading(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            UiErrors.show(CompleteGoogleSignupActivity.this, response,
                                    getString(R.string.error_google_register_failed));
                            return;
                        }

                        onRegistered(response.body());
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(CompleteGoogleSignupActivity.this,
                                R.string.error_google_register_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onRegistered(LoginResponse login) {

        if (!Role.isSupported(login.getRole())) {
            Toast.makeText(this, R.string.error_unsupported_role, Toast.LENGTH_LONG).show();
            return;
        }

        tokenManager.saveSession(
                login.getAccessToken(),
                login.getRefreshToken(),
                login.getRole(),
                login.getCompanyId(),
                login.getUserId(),
                login.getFirstName());

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Blank optional fields go as null, not "" — the API treats them as absent. */
    private String text(CharSequence value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnFinishSignup.setEnabled(!loading);
    }
}
