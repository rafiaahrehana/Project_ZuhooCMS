package com.raf.zuhoo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.model.response.GoogleSignInResponse;
import com.raf.zuhoo.data.model.response.LoginResponse;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.databinding.ActivityLoginBinding;
import com.raf.zuhoo.ui.dashboard.DashboardActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private AuthRepository authRepository;
    private TokenManager tokenManager;
    private GoogleSignInHelper googleSignInHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenManager = new TokenManager(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        googleSignInHelper = new GoogleSignInHelper(this);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
        binding.btnJoinCompany.setOnClickListener(v ->
                startActivity(new Intent(this, ClientRegisterActivity.class)));
        binding.btnCreateCompany.setOnClickListener(v ->
                startActivity(new Intent(this, CompanyOwnerRegisterActivity.class)));
        binding.btnForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.btnVerifyEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, VerifyEmailActivity.class);
            // Carry whatever they've already typed so they don't retype it to resend.
            intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL,
                    binding.emailEditText.getText() == null
                            ? null : binding.emailEditText.getText().toString().trim());
            startActivity(intent);
        });

        // Re-arm the expiry guard now that we're back on the login screen, so a *future* session
        // can route here too.
        SessionExpiry.reset();

        if (getIntent().getBooleanExtra(SessionExpiry.EXTRA_SESSION_EXPIRED, false)) {
            Toast.makeText(this, R.string.error_session_expired, Toast.LENGTH_LONG).show();
            // Session is already cleared by SessionExpiry — fall through to the plain form
            // rather than the auto-forward/biometric path below.
            return;
        }

        if (tokenManager.isLoggedIn()) {
            if (tokenManager.isBiometricEnabled()) {
                // The plain login form stays visible underneath as a fallback if the user
                // cancels or fails biometric auth — they're never stuck with no way in.
                showBiometricPrompt();
            } else {
                goToDashboard();
            }
        }
    }

    private void showBiometricPrompt() {

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                goToDashboard();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Cancelled or unavailable — fall back to the plain login form already on screen.
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginActivity.this, R.string.error_biometric_failed, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.action_no))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void attemptLogin() {

        String email = binding.emailEditText.getText() == null
                ? "" : binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText() == null
                ? "" : binding.passwordEditText.getText().toString().trim();

        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.emailInputLayout.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordInputLayout.setError(getString(R.string.error_password_required));
            return;
        }

        setLoading(true);

        authRepository.login(email, password, new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    showError(response);
                    return;
                }

                onLoginSuccess(response.body());
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startGoogleSignIn() {

        setLoading(true);
        binding.btnGoogleSignIn.setEnabled(false);

        googleSignInHelper.signIn(this, new GoogleSignInHelper.Callback() {

            @Override
            public void onToken(String firebaseIdToken) {
                exchangeGoogleToken(firebaseIdToken);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                binding.btnGoogleSignIn.setEnabled(true);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled() {
                // Backing out of the account chooser isn't a failure — just restore the form.
                setLoading(false);
                binding.btnGoogleSignIn.setEnabled(true);
            }
        });
    }

    /** Hands the verified Google identity to our backend, which decides log-in vs sign-up. */
    private void exchangeGoogleToken(String firebaseIdToken) {

        authRepository.googleSignIn(firebaseIdToken, new Callback<GoogleSignInResponse>() {

            @Override
            public void onResponse(Call<GoogleSignInResponse> call,
                                   Response<GoogleSignInResponse> response) {

                setLoading(false);
                binding.btnGoogleSignIn.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(LoginActivity.this, response,
                            getString(R.string.error_google_signin_failed));
                    return;
                }

                GoogleSignInResponse body = response.body();

                if (body.isRegistered() && body.getLogin() != null) {
                    onLoginSuccess(body.getLogin());
                    return;
                }

                // Genuine Google account, but nobody in this system uses that email yet. Send
                // them on to pick a company — we can't guess which tenant they belong to.
                Intent intent = new Intent(LoginActivity.this, CompleteGoogleSignupActivity.class);
                intent.putExtra(CompleteGoogleSignupActivity.EXTRA_ID_TOKEN, firebaseIdToken);
                intent.putExtra(CompleteGoogleSignupActivity.EXTRA_EMAIL, body.getEmail());
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<GoogleSignInResponse> call, Throwable t) {
                setLoading(false);
                binding.btnGoogleSignIn.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        R.string.error_google_signin_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onLoginSuccess(LoginResponse login) {

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

        goToDashboard();
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(Response<LoginResponse> response) {

        String message = ApiErrors.extractMessage(response, getString(R.string.error_login_failed));

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }
}
