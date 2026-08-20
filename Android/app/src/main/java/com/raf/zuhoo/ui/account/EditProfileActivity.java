package com.raf.zuhoo.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.SecureScreen;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.response.UserProfileResponse;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.data.repository.UserProfileRepository;
import com.raf.zuhoo.databinding.ActivityEditProfileBinding;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private UserProfileRepository userProfileRepository;
    private AuthRepository authRepository;

    private String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shows money / personal details - keep it out of screenshots and recents.
        SecureScreen.apply(this);

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userProfileRepository = new UserProfileRepository(this);
        authRepository = new AuthRepository(this);

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        binding.btnChangePassword.setOnClickListener(v -> changePassword());

        loadProfile();
    }

    private void loadProfile() {

        binding.profileProgressBar.setVisibility(View.VISIBLE);

        userProfileRepository.getProfile(new Callback<UserProfileResponse>() {

            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {

                binding.profileProgressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(EditProfileActivity.this,
                            R.string.error_profile_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                UserProfileResponse profile = response.body();
                currentEmail = profile.getEmail();

                binding.firstNameEditText.setText(profile.getFirstName());
                binding.lastNameEditText.setText(profile.getLastName());
                binding.emailEditText.setText(profile.getEmail());
                binding.phoneEditText.setText(profile.getPhone());
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                binding.profileProgressBar.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this,
                        R.string.error_profile_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveProfile() {

        String firstName = textOf(binding.firstNameEditText);
        String lastName = textOf(binding.lastNameEditText);
        String email = textOf(binding.emailEditText);
        String phone = textOf(binding.phoneEditText);

        binding.firstNameInputLayout.setError(null);
        binding.lastNameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);

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

        boolean emailChanged = !email.equalsIgnoreCase(currentEmail);
        String currentPassword = textOf(binding.currentPasswordForEmailEditText);

        if (emailChanged && TextUtils.isEmpty(currentPassword)) {
            binding.currentPasswordForEmailInputLayout.setError(
                    getString(R.string.error_password_required));
            return;
        }

        setProfileLoading(true);

        userProfileRepository.updateProfile(firstName, lastName, email,
                TextUtils.isEmpty(phone) ? null : phone,
                emailChanged ? currentPassword : null,
                new Callback<UserProfileResponse>() {

            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {

                setProfileLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(EditProfileActivity.this, response, getString(R.string.error_profile_update_failed));
                    return;
                }

                currentEmail = response.body().getEmail();
                binding.currentPasswordForEmailEditText.setText("");
                Toast.makeText(EditProfileActivity.this,
                        R.string.profile_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                setProfileLoading(false);
                Toast.makeText(EditProfileActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void changePassword() {

        String currentPassword = textOf(binding.currentPasswordEditText);
        String newPassword = textOf(binding.newPasswordEditText);
        String confirmPassword = textOf(binding.confirmPasswordEditText);

        binding.currentPasswordInputLayout.setError(null);
        binding.newPasswordInputLayout.setError(null);
        binding.confirmPasswordInputLayout.setError(null);

        if (TextUtils.isEmpty(currentPassword)) {
            binding.currentPasswordInputLayout.setError(getString(R.string.error_password_required));
            return;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 8) {
            binding.newPasswordInputLayout.setError(getString(R.string.helper_password_min_length));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.confirmPasswordInputLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }

        setPasswordLoading(true);

        authRepository.changePassword(currentPassword, newPassword, confirmPassword,
                new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                setPasswordLoading(false);

                if (!response.isSuccessful()) {
                    UiErrors.show(EditProfileActivity.this, response, getString(R.string.error_change_password_failed));
                    return;
                }

                binding.currentPasswordEditText.setText("");
                binding.newPasswordEditText.setText("");
                binding.confirmPasswordEditText.setText("");
                Toast.makeText(EditProfileActivity.this,
                        R.string.password_changed, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setPasswordLoading(false);
                Toast.makeText(EditProfileActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void setProfileLoading(boolean loading) {
        binding.profileProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!loading);
    }

    private void setPasswordLoading(boolean loading) {
        binding.passwordProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnChangePassword.setEnabled(!loading);
    }
}
