package com.raf.zuhoo.ui.attendance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.AttendanceResponse;
import com.raf.zuhoo.data.repository.AttendanceRepository;
import com.raf.zuhoo.databinding.ActivityCheckInBinding;
import com.raf.zuhoo.ui.common.SelfieCapture;
import com.raf.zuhoo.ui.common.UiErrors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Employee self check-in/out: requires a live camera selfie and a GPS fix before either button
 * enables, so a check-in can't be faked from a device sitting on a desk at home. The server is
 * the actual authority on whether the location is close enough to the office — this screen just
 * won't submit without both pieces of evidence attached.
 */
public class CheckInActivity extends AppCompatActivity {

    private ActivityCheckInBinding binding;
    private AttendanceRepository attendanceRepository;
    private SelfieCapture selfieCapture;
    private LocationHelper locationHelper;

    private Double latitude;
    private Double longitude;
    private Long todayAttendanceId;
    private boolean checkedInToday;
    private boolean checkedOutToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCheckInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        attendanceRepository = new AttendanceRepository(this);
        locationHelper = new LocationHelper(this);

        selfieCapture = new SelfieCapture(this, binding.selfiePreview, new SelfieCapture.Listener() {
            @Override public void onUploaded(String url) {
                updateActionButtonState();
            }
            @Override public void onCleared() {
                updateActionButtonState();
            }
        });

        binding.btnTakeSelfie.setOnClickListener(v -> selfieCapture.capture());
        binding.btnCheckIn.setOnClickListener(v -> checkIn());
        binding.btnCheckOut.setOnClickListener(v -> checkOut());

        requestLocation();
        loadToday();
    }

    private void requestLocation() {

        binding.locationStatusText.setText(R.string.status_location_pending);

        locationHelper.requestLocation(new LocationHelper.Listener() {
            @Override public void onLocation(double lat, double lng) {
                latitude = lat;
                longitude = lng;
                binding.locationStatusText.setText(R.string.status_location_captured);
                updateActionButtonState();
            }

            @Override public void onUnavailable() {
                binding.locationStatusText.setText(R.string.status_location_unavailable);
                updateActionButtonState();
            }
        });
    }

    private void loadToday() {

        binding.progressBar.setVisibility(View.VISIBLE);

        attendanceRepository.getMyTodayAttendance(new Callback<AttendanceResponse>() {

            @Override
            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {

                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    UiErrors.show(CheckInActivity.this, response, getString(R.string.error_attendance_load_failed));
                    return;
                }

                bindToday(response.body());
            }

            @Override
            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CheckInActivity.this, R.string.error_attendance_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** response is null when nothing has been recorded for today yet. */
    private void bindToday(AttendanceResponse today) {

        if (today == null) {
            todayAttendanceId = null;
            checkedInToday = false;
            checkedOutToday = false;
            binding.todayStatusText.setText(R.string.status_not_checked_in);
            binding.flaggedBanner.setVisibility(View.GONE);
            showCheckInButton();
            return;
        }

        todayAttendanceId = today.getId();
        checkedInToday = today.isCheckedIn();
        checkedOutToday = today.isCheckedOut();

        if (checkedOutToday) {
            binding.todayStatusText.setText(getString(R.string.status_checked_out,
                    today.getCheckInTime(), today.getCheckOutTime()));
        } else if (checkedInToday) {
            binding.todayStatusText.setText(getString(R.string.status_checked_in_at, today.getCheckInTime()));
            showCheckOutButton();
        } else {
            binding.todayStatusText.setText(R.string.status_not_checked_in);
            showCheckInButton();
        }

        if (today.isLocationFlagged()) {
            binding.flaggedBanner.setVisibility(View.VISIBLE);
            binding.flaggedBanner.setText(
                    getString(R.string.status_location_flagged, today.getLocationFlagReason()));
        } else {
            binding.flaggedBanner.setVisibility(View.GONE);
        }

        if (checkedOutToday) {
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.btnCheckOut.setVisibility(View.GONE);
            binding.btnTakeSelfie.setEnabled(false);
        }
    }

    private void showCheckInButton() {
        binding.btnCheckIn.setVisibility(View.VISIBLE);
        binding.btnCheckOut.setVisibility(View.GONE);
    }

    private void showCheckOutButton() {
        binding.btnCheckIn.setVisibility(View.GONE);
        binding.btnCheckOut.setVisibility(View.VISIBLE);
    }

    private void updateActionButtonState() {
        boolean ready = selfieCapture.url() != null && latitude != null && longitude != null;
        binding.btnCheckIn.setEnabled(ready && !checkedInToday);
        binding.btnCheckOut.setEnabled(ready && checkedInToday && !checkedOutToday);
    }

    private void checkIn() {

        setBusy(true);

        attendanceRepository.checkIn(String.valueOf(latitude), String.valueOf(longitude),
                selfieCapture.url(), new Callback<AttendanceResponse>() {

            @Override
            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {

                setBusy(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CheckInActivity.this, response, getString(R.string.error_check_in_failed));
                    updateActionButtonState();
                    return;
                }

                Toast.makeText(CheckInActivity.this, R.string.check_in_success, Toast.LENGTH_SHORT).show();
                selfieCapture.clear();
                bindToday(response.body());
                updateActionButtonState();
            }

            @Override
            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                setBusy(false);
                updateActionButtonState();
                Toast.makeText(CheckInActivity.this, R.string.error_check_in_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkOut() {

        if (todayAttendanceId == null) {
            return;
        }

        setBusy(true);

        attendanceRepository.checkOut(todayAttendanceId, String.valueOf(latitude), String.valueOf(longitude),
                selfieCapture.url(), new Callback<AttendanceResponse>() {

            @Override
            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {

                setBusy(false);

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(CheckInActivity.this, response, getString(R.string.error_check_out_failed));
                    updateActionButtonState();
                    return;
                }

                Toast.makeText(CheckInActivity.this, R.string.check_out_success, Toast.LENGTH_SHORT).show();
                selfieCapture.clear();
                bindToday(response.body());
                updateActionButtonState();
            }

            @Override
            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                setBusy(false);
                updateActionButtonState();
                Toast.makeText(CheckInActivity.this, R.string.error_check_out_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Only the progress spinner and the selfie button toggle here — updateActionButtonState()
     *  (called separately once the request resolves) is the single source of truth for whether
     *  check-in/out should be enabled, so this never has to read back its own prior state. */
    private void setBusy(boolean busy) {
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) {
            binding.btnCheckIn.setEnabled(false);
            binding.btnCheckOut.setEnabled(false);
        }
        binding.btnTakeSelfie.setEnabled(!busy);
    }
}
