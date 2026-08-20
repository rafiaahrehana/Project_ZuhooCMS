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
import com.raf.zuhoo.databinding.ActivityForgotPasswordBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        binding.btnSendResetLink.setOnClickListener(v -> sendResetLink());
        binding.btnHaveCode.setOnClickListener(v -> openResetScreen(null));
    }

    private void sendResetLink() {

        String email = binding.emailEditText.getText() == null
                ? "" : binding.emailEditText.getText().toString().trim();

        binding.emailInputLayout.setError(null);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.error_email_invalid));
            return;
        }

        setLoading(true);

        authRepository.forgotPassword(email, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                setLoading(false);

                if (!response.isSuccessful()) {
                    UiErrors.show(ForgotPasswordActivity.this, response,
                            getString(R.string.error_reset_link_failed));
                    return;
                }

                // The backend answers identically whether or not the address exists, so that a
                // stranger can't use this screen to find out who has an account. Mirror that
                // here — don't imply the email was found.
                Toast.makeText(ForgotPasswordActivity.this,
                        R.string.reset_link_sent, Toast.LENGTH_LONG).show();

                openResetScreen(email);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        R.string.error_reset_link_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openResetScreen(String email) {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL, email);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSendResetLink.setEnabled(!loading);
    }
}
