package com.raf.zuhoo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.PasswordPolicy;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.databinding.ActivityResetPasswordBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    // Carried through purely so the screen can be reached from the forgot-password step without
    // the user retyping anything; the reset call itself is keyed on the token, not the email.
    public static final String EXTRA_EMAIL = "extra_email";

    private ActivityResetPasswordBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository(this);

        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {

        String token = text(binding.tokenEditText.getText());
        String password = text(binding.passwordEditText.getText());
        String confirm = text(binding.confirmEditText.getText());

        binding.tokenInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);
        binding.confirmInputLayout.setError(null);

        if (TextUtils.isEmpty(token)) {
            binding.tokenInputLayout.setError(getString(R.string.error_reset_code_required));
            return;
        }

        // The reset endpoint only enforces a minimum length, but hold the same complexity rule
        // the registration screens use — a password that passes here should not be one the user
        // would have been refused at signup.
        if (!PasswordPolicy.isValid(password)) {
            binding.passwordInputLayout.setError(getString(R.string.error_password_policy));
            return;
        }

        if (!password.equals(confirm)) {
            binding.confirmInputLayout.setError(getString(R.string.error_passwords_do_not_match));
            return;
        }

        setLoading(true);

        authRepository.resetPassword(token, password, confirm, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                setLoading(false);

                if (!response.isSuccessful()) {
                    UiErrors.show(ResetPasswordActivity.this, response,
                            getString(R.string.error_reset_password_failed));
                    return;
                }

                Toast.makeText(ResetPasswordActivity.this,
                        R.string.reset_password_success, Toast.LENGTH_LONG).show();

                goToLogin();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ResetPasswordActivity.this,
                        R.string.error_reset_password_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnResetPassword.setEnabled(!loading);
    }
}
