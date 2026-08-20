package com.raf.zuhoo.ui.noticeboard;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.AnnouncementResponse;
import com.raf.zuhoo.data.model.response.HolidayResponse;
import com.raf.zuhoo.data.repository.NoticeBoardRepository;
import com.raf.zuhoo.databinding.ActivityNoticeBoardBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Company announcements + this year's holiday calendar — both read-only, no ViewModel needed
 * (same reasoning as PayslipListActivity). The two lists load independently so a failure in one
 * doesn't block the other.
 */
public class NoticeBoardActivity extends AppCompatActivity {

    private ActivityNoticeBoardBinding binding;
    private NoticeBoardRepository repository;
    private AnnouncementAdapter announcementAdapter;
    private HolidayAdapter holidayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNoticeBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new NoticeBoardRepository(this);

        announcementAdapter = new AnnouncementAdapter(new ArrayList<>(), this::openAnnouncementDialog);
        binding.announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.announcementsRecyclerView.setAdapter(announcementAdapter);

        holidayAdapter = new HolidayAdapter(new ArrayList<>());
        binding.holidaysRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.holidaysRecyclerView.setAdapter(holidayAdapter);

        loadAnnouncements();
        loadHolidays();
    }

    private void loadAnnouncements() {

        binding.stateView.showLoading();

        repository.getActiveAnnouncements(new Callback<List<AnnouncementResponse>>() {

            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NoticeBoardActivity.this, R.string.error_announcements_load_failed,
                            Toast.LENGTH_LONG).show();
                    if (announcementAdapter.getItemCount() == 0) {
                        binding.stateView.showContent();
                    }
                    return;
                }

                announcementAdapter.submitList(response.body());
                if (response.body().isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_inbox,
                            R.string.empty_announcements, R.string.empty_announcements_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Toast.makeText(NoticeBoardActivity.this, R.string.error_announcements_load_failed,
                        Toast.LENGTH_LONG).show();
                if (announcementAdapter.getItemCount() == 0) {
                    binding.stateView.showContent();
                }
            }
        });
    }

    private void loadHolidays() {

        repository.getCurrentYearHolidays(new Callback<List<HolidayResponse>>() {

            @Override
            public void onResponse(Call<List<HolidayResponse>> call, Response<List<HolidayResponse>> response) {

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    return;
                }

                holidayAdapter.submitList(response.body());
                binding.holidaysLabel.setVisibility(View.VISIBLE);
                binding.holidaysRecyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<HolidayResponse>> call, Throwable t) {
                // The holiday strip is a secondary section here — the announcements feed below
                // is the main content, so a failed holiday load just leaves the strip hidden.
            }
        });
    }

    private void openAnnouncementDialog(AnnouncementResponse announcement) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(announcement.getTitle())
                .setMessage(announcement.getBody())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
