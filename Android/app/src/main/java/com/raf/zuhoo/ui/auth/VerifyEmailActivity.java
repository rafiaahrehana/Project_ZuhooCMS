package com.raf.zuhoo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.databinding.ActivityVerifyEmailBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyEmailActivity extends AppCompatActivity {

    // Pre-fills the resend field when we get here straight after registering.
    public static final String EXTRA_EMAIL = "extra_email";

    private ActivityVerifyEmailBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            binding.emailEditText.setText(email);
        }

        binding.btnVerify.setOnClickListener(v -> verify());
        binding.btnResend.setOnClickListener(v -> resend());
    }

    private void verify() {

        String token = text(binding.tokenEditText.getText());

        binding.tokenInputLayout.setError(null);

        if (TextUtils.isEmpty(token)) {
            binding.tokenInputLayout.setError(getString(R.string.error_verification_code_required));
            return;
        }

        setLoading(true);

        authRepository.verifyEmail(token, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                setLoading(false);

                if (!response.isSuccessful()) {
                    UiErrors.show(VerifyEmailActivity.this, response,
                            getString(R.string.error_verify_email_failed));
                    return;
                }

                Toast.makeText(VerifyEmailActivity.this,
                        R.string.verify_email_success, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                Toast.makeText(VerifyEmailActivity.this,
                        R.string.error_verify_email_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resend() {

        String email = text(binding.emailEditText.getText());

        binding.emailInputLayout.setError(null);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.error_email_invalid));
            return;
        }

        binding.btnResend.setEnabled(false);

        authRepository.resendVerification(email, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                binding.btnResend.setEnabled(true);

                if (!response.isSuccessful()) {
                    UiErrors.show(VerifyEmailActivity.this, response,
                            getString(R.string.error_resend_verification_failed));
                    return;
                }

                Toast.makeText(VerifyEmailActivity.this,
                        R.string.resend_verification_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                binding.btnResend.setEnabled(true);
                Toast.makeText(VerifyEmailActivity.this,
                        R.string.error_resend_verification_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVerify.setEnabled(!loading);
    }
}
