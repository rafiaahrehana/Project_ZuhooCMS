package com.raf.zuhoo.ui.notification;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.request.UpdateNotificationPreferenceRequest;
import com.raf.zuhoo.data.model.response.NotificationPreferenceResponse;
import com.raf.zuhoo.data.repository.NotificationRepository;
import com.raf.zuhoo.databinding.ActivityNotificationPreferencesBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationPreferencesActivity extends AppCompatActivity {

    private ActivityNotificationPreferencesBinding binding;
    private NotificationRepository notificationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNotificationPreferencesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notificationRepository = new NotificationRepository(this);

        binding.btnSave.setOnClickListener(v -> save());

        load();
    }

    private void load() {

        binding.progressBar.setVisibility(View.VISIBLE);

        notificationRepository.getPreferences(new Callback<NotificationPreferenceResponse>() {

            @Override
            public void onResponse(Call<NotificationPreferenceResponse> call,
                                   Response<NotificationPreferenceResponse> response) {

                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NotificationPreferencesActivity.this,
                            R.string.error_preferences_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                NotificationPreferenceResponse prefs = response.body();
                binding.switchEmailServiceRequest.setChecked(prefs.isEmailOnServiceRequest());
                binding.switchEmailStatusChange.setChecked(prefs.isEmailOnStatusChange());
                binding.switchEmailInvoice.setChecked(prefs.isEmailOnInvoice());
                binding.switchEmailPayment.setChecked(prefs.isEmailOnPayment());
                binding.switchEmailTaskAssigned.setChecked(prefs.isEmailOnTaskAssigned());
                binding.switchEmailLeaveUpdate.setChecked(prefs.isEmailOnLeaveUpdate());
                binding.switchInAppServiceRequest.setChecked(prefs.isInAppOnServiceRequest());
                binding.switchInAppStatusChange.setChecked(prefs.isInAppOnStatusChange());
                binding.switchEmailMarketing.setChecked(prefs.isEmailMarketing());
            }

            @Override
            public void onFailure(Call<NotificationPreferenceResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationPreferencesActivity.this,
                        R.string.error_preferences_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save() {

        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(
                binding.switchEmailServiceRequest.isChecked(),
                binding.switchEmailStatusChange.isChecked(),
                binding.switchEmailInvoice.isChecked(),
                binding.switchEmailPayment.isChecked(),
                binding.switchEmailTaskAssigned.isChecked(),
                binding.switchEmailLeaveUpdate.isChecked(),
                binding.switchInAppServiceRequest.isChecked(),
                binding.switchInAppStatusChange.isChecked(),
                binding.switchEmailMarketing.isChecked());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        notificationRepository.updatePreferences(request, new Callback<NotificationPreferenceResponse>() {

            @Override
            public void onResponse(Call<NotificationPreferenceResponse> call,
                                   Response<NotificationPreferenceResponse> response) {

                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(NotificationPreferencesActivity.this, response, getString(R.string.error_preferences_update_failed));
                    return;
                }

                Toast.makeText(NotificationPreferencesActivity.this,
                        R.string.preferences_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<NotificationPreferenceResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                Toast.makeText(NotificationPreferencesActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
