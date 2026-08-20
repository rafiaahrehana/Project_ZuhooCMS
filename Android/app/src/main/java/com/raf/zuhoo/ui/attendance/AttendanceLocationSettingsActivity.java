package com.raf.zuhoo.ui.attendance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.AttendanceLocationSettingsResponse;
import com.raf.zuhoo.data.repository.AttendanceRepository;
import com.raf.zuhoo.databinding.ActivityAttendanceLocationSettingsBinding;
import com.raf.zuhoo.ui.common.UiErrors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * COMPANY_OWNER-only screen (see AccountActivity) for the office location that attendance
 * check-ins are measured against. Enforcement defaults off server-side, so leaving this unset
 * is a valid, safe state — nothing here is required before the app is otherwise usable.
 */
public class AttendanceLocationSettingsActivity extends AppCompatActivity {

    private ActivityAttendanceLocationSettingsBinding binding;
    private AttendanceRepository attendanceRepository;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAttendanceLocationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        attendanceRepository = new AttendanceRepository(this);
        locationHelper = new LocationHelper(this);

        binding.btnUseCurrentLocation.setOnClickListener(v -> useCurrentLocation());
        binding.btnSave.setOnClickListener(v -> save());

        load();
    }

    private void load() {

        binding.progressBar.setVisibility(View.VISIBLE);

        attendanceRepository.getLocationSettings(new Callback<AttendanceLocationSettingsResponse>() {

            @Override
            public void onResponse(Call<AttendanceLocationSettingsResponse> call,
                                   Response<AttendanceLocationSettingsResponse> response) {

                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(AttendanceLocationSettingsActivity.this, response,
                            getString(R.string.error_settings_load_failed));
                    return;
                }

                AttendanceLocationSettingsResponse settings = response.body();
                binding.switchEnforcement.setChecked(settings.isGpsEnforcementEnabled());
                if (settings.getOfficeLatitude() != null) {
                    binding.latitudeEditText.setText(String.valueOf(settings.getOfficeLatitude()));
                }
                if (settings.getOfficeLongitude() != null) {
                    binding.longitudeEditText.setText(String.valueOf(settings.getOfficeLongitude()));
                }
                if (settings.getRadiusMeters() != null) {
                    binding.radiusEditText.setText(String.valueOf(settings.getRadiusMeters()));
                }
            }

            @Override
            public void onFailure(Call<AttendanceLocationSettingsResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AttendanceLocationSettingsActivity.this,
                        R.string.error_settings_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void useCurrentLocation() {

        binding.btnUseCurrentLocation.setEnabled(false);

        locationHelper.requestLocation(new LocationHelper.Listener() {
            @Override public void onLocation(double latitude, double longitude) {
                binding.btnUseCurrentLocation.setEnabled(true);
                binding.latitudeEditText.setText(String.valueOf(latitude));
                binding.longitudeEditText.setText(String.valueOf(longitude));
            }

            @Override public void onUnavailable() {
                binding.btnUseCurrentLocation.setEnabled(true);
                Toast.makeText(AttendanceLocationSettingsActivity.this,
                        R.string.status_location_unavailable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save() {

        boolean enforcementEnabled = binding.switchEnforcement.isChecked();
        String latitudeText = text(binding.latitudeEditText);
        String longitudeText = text(binding.longitudeEditText);
        String radiusText = text(binding.radiusEditText);

        if (enforcementEnabled && (latitudeText.isEmpty() || longitudeText.isEmpty())) {
            Toast.makeText(this, R.string.error_office_location_required, Toast.LENGTH_LONG).show();
            return;
        }

        double latitude;
        double longitude;
        int radius;
        try {
            latitude = latitudeText.isEmpty() ? 0 : Double.parseDouble(latitudeText);
            longitude = longitudeText.isEmpty() ? 0 : Double.parseDouble(longitudeText);
            radius = radiusText.isEmpty() ? 200 : Integer.parseInt(radiusText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_location_input, Toast.LENGTH_LONG).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        attendanceRepository.updateLocationSettings(latitude, longitude, radius, enforcementEnabled,
                new Callback<AttendanceLocationSettingsResponse>() {

            @Override
            public void onResponse(Call<AttendanceLocationSettingsResponse> call,
                                   Response<AttendanceLocationSettingsResponse> response) {

                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(AttendanceLocationSettingsActivity.this, response,
                            getString(R.string.error_settings_save_failed));
                    return;
                }

                Toast.makeText(AttendanceLocationSettingsActivity.this,
                        R.string.settings_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AttendanceLocationSettingsResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                Toast.makeText(AttendanceLocationSettingsActivity.this,
                        R.string.error_settings_save_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
