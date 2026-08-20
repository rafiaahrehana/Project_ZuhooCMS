package com.raf.zuhoo.ui.account;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.core.os.LocaleListCompat;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.ZuhooApplication;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.Role;
import com.raf.zuhoo.data.repository.AuthRepository;
import com.raf.zuhoo.data.repository.DeviceTokenRepository;
import com.raf.zuhoo.databinding.ActivityAccountBinding;
import com.raf.zuhoo.ui.attendance.AttendanceLocationSettingsActivity;
import com.raf.zuhoo.ui.auth.GoogleSignInHelper;
import com.raf.zuhoo.ui.auth.LoginActivity;
import com.raf.zuhoo.ui.directory.DirectoryActivity;
import com.raf.zuhoo.ui.kb.KbArticleListActivity;
import com.raf.zuhoo.ui.overview.CompanyOverviewActivity;
import com.raf.zuhoo.ui.notification.NotificationListActivity;
import com.raf.zuhoo.ui.notification.NotificationPreferencesActivity;
import com.raf.zuhoo.ui.wallet.WalletActivity;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends BottomNavActivity {

    private static final String[] LANGUAGE_LABELS = {"English", "বাংলা (Bengali)"};
    private static final String[] LANGUAGE_TAGS = {"en", "bn"};

    private ActivityAccountBinding binding;
    private TokenManager tokenManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);
        authRepository = new AuthRepository(this);

        if (Role.CLIENT.equals(tokenManager.getRole())) {
            binding.btnCompanyProfile.setVisibility(View.VISIBLE);
        }
        if (Role.COMPANY_OWNER.equals(tokenManager.getRole())
                || Role.EMPLOYEE.equals(tokenManager.getRole())) {
            binding.btnWallet.setVisibility(View.VISIBLE);
            binding.btnDirectory.setVisibility(View.VISIBLE);
        }
        if (Role.COMPANY_OWNER.equals(tokenManager.getRole())) {
            binding.btnAttendanceLocationSettings.setVisibility(View.VISIBLE);
            binding.btnCompanyOverview.setVisibility(View.VISIBLE);
        }

        setUpBiometricSwitch();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));
        binding.btnCompanyProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ClientProfileActivity.class)));
        binding.btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationListActivity.class)));
        binding.btnNotificationPreferences.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationPreferencesActivity.class)));
        binding.btnHelpCenter.setOnClickListener(v ->
                startActivity(new Intent(this, KbArticleListActivity.class)));
        binding.btnChangeLanguage.setOnClickListener(v -> promptLanguage());
        binding.btnAttendanceLocationSettings.setOnClickListener(v ->
                startActivity(new Intent(this, AttendanceLocationSettingsActivity.class)));
        binding.btnCompanyOverview.setOnClickListener(v ->
                startActivity(new Intent(this, CompanyOverviewActivity.class)));
        binding.btnWallet.setOnClickListener(v ->
                startActivity(new Intent(this, WalletActivity.class)));
        binding.btnDirectory.setOnClickListener(v ->
                startActivity(new Intent(this, DirectoryActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void setUpBiometricSwitch() {

        BiometricManager biometricManager = BiometricManager.from(this);
        boolean canUseBiometrics = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (!canUseBiometrics) {
            return;
        }

        binding.switchBiometric.setVisibility(View.VISIBLE);
        binding.switchBiometric.setChecked(tokenManager.isBiometricEnabled());
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
                tokenManager.setBiometricEnabled(isChecked));
    }

    private void promptLanguage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_language_title)
                .setItems(LANGUAGE_LABELS, (dialog, which) -> {
                    LocaleListCompat locales = LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[which]);
                    AppCompatDelegate.setApplicationLocales(locales);
                })
                .show();
    }

    private void logout() {

        String refreshToken = tokenManager.getRefreshToken();

        binding.btnLogout.setEnabled(false);

        // Must go out before the session is cleared — the call needs the JWT to authenticate.
        // Fire-and-forget: a failure here can't be allowed to block signing out.
        new DeviceTokenRepository(this).unregister();

        if (refreshToken == null) {
            finishLogout();
            return;
        }

        authRepository.logout(refreshToken, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                finishLogout();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Even if the revoke call fails (offline, etc.), still clear the local session —
                // staying logged in locally because a network call failed would be worse.
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        // Close the chat socket before the token goes — it authenticates with that token, and
        // leaving it open would keep a per-user queue alive for a signed-out session.
        // Firebase keeps its own session alongside ours. Leaving it signed in means the next
        // "Continue with Google" silently reuses the previous account instead of asking.
        GoogleSignInHelper.signOut();
        ZuhooApplication.graph().notificationCenter().stop();
        ZuhooApplication.graph().chatSocket().shutdown();
        // The cache holds this user's requests, invoices and notifications — it must not survive
        // into whoever signs in next on this device.
        ZuhooApplication.graph().listCache().wipe();
        tokenManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_account;
    }
}
