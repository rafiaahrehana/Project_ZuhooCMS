package com.raf.zuhoo.ui.overview;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.FinanceDashboardResponse;
import com.raf.zuhoo.data.model.response.HrDashboardResponse;
import com.raf.zuhoo.data.repository.CompanyOverviewRepository;
import com.raf.zuhoo.databinding.ActivityCompanyOverviewBinding;
import com.raf.zuhoo.databinding.ItemOverviewStatBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * COMPANY_OWNER-only quick glance at this month's finance and today's HR figures — plain numbers,
 * no charts, mirroring the web dashboard's stat-card style rather than its trend/budget charts
 * (both backend responses carry those too; this screen only asks for the top-line fields it
 * needs, matching the trimmed response models). Not staff-wide like Leave/Expense approvals —
 * this is company-level financial visibility, so it stays owner-only, same as
 * AttendanceLocationSettingsActivity.
 */
public class CompanyOverviewActivity extends AppCompatActivity {

    private ActivityCompanyOverviewBinding binding;
    private CompanyOverviewRepository repository;

    private boolean financeLoaded;
    private boolean hrLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompanyOverviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new CompanyOverviewRepository(this);

        bindStat(binding.statRevenue, R.string.stat_revenue, R.drawable.ic_wallet);
        bindStat(binding.statExpenses, R.string.stat_expenses, R.drawable.ic_receipt);
        bindStat(binding.statNetProfit, R.string.stat_net_profit, R.drawable.ic_check_circle);
        bindStat(binding.statCashCollected, R.string.stat_cash_collected, R.drawable.ic_billing);
        bindStat(binding.statOutstanding, R.string.stat_outstanding, R.drawable.ic_document);
        bindStat(binding.statOverdue, R.string.stat_overdue, R.drawable.ic_alert);
        bindStat(binding.statTotalEmployees, R.string.stat_total_employees, R.drawable.ic_briefcase);
        bindStat(binding.statPresentToday, R.string.stat_present_today, R.drawable.ic_check_circle);
        bindStat(binding.statOnLeaveToday, R.string.stat_on_leave_today, R.drawable.ic_calendar);
        bindStat(binding.statAbsentToday, R.string.stat_absent_today, R.drawable.ic_cancel);
        bindStat(binding.statOpenPositions, R.string.stat_open_positions, R.drawable.ic_inbox);
        bindStat(binding.statPendingApprovals, R.string.stat_pending_leave_approvals, R.drawable.ic_clock);

        binding.progressBar.setVisibility(View.VISIBLE);
        loadFinance();
        loadHr();
    }

    private void bindStat(ItemOverviewStatBinding stat, int labelRes, int iconRes) {
        stat.statLabel.setText(labelRes);
        stat.statIcon.setImageResource(iconRes);
    }

    private void loadFinance() {

        repository.getFinanceDashboard(new Callback<FinanceDashboardResponse>() {

            @Override
            public void onResponse(Call<FinanceDashboardResponse> call, Response<FinanceDashboardResponse> response) {

                financeLoaded = true;
                updateProgress();

                if (!response.isSuccessful() || response.body() == null) {
                    showFinanceUnavailable();
                    return;
                }

                FinanceDashboardResponse dashboard = response.body();
                binding.statRevenue.statValue.setText(String.valueOf(dashboard.getTotalRevenue()));
                binding.statExpenses.statValue.setText(String.valueOf(dashboard.getTotalExpenses()));
                binding.statNetProfit.statValue.setText(String.valueOf(dashboard.getNetProfit()));
                binding.statCashCollected.statValue.setText(String.valueOf(dashboard.getCashCollected()));
                binding.statOutstanding.statValue.setText(String.valueOf(dashboard.getOutstanding()));
                binding.statOverdue.statValue.setText(String.valueOf(dashboard.getOverdue()));
            }

            @Override
            public void onFailure(Call<FinanceDashboardResponse> call, Throwable t) {
                financeLoaded = true;
                updateProgress();
                showFinanceUnavailable();
            }
        });
    }

    private void loadHr() {

        repository.getHrDashboard(new Callback<HrDashboardResponse>() {

            @Override
            public void onResponse(Call<HrDashboardResponse> call, Response<HrDashboardResponse> response) {

                hrLoaded = true;
                updateProgress();

                if (!response.isSuccessful() || response.body() == null) {
                    showHrUnavailable();
                    return;
                }

                HrDashboardResponse dashboard = response.body();
                binding.statTotalEmployees.statValue.setText(String.valueOf(dashboard.getTotalEmployees()));
                binding.statPresentToday.statValue.setText(String.valueOf(dashboard.getPresentToday()));
                binding.statOnLeaveToday.statValue.setText(String.valueOf(dashboard.getOnLeaveToday()));
                binding.statAbsentToday.statValue.setText(String.valueOf(dashboard.getAbsentToday()));
                binding.statOpenPositions.statValue.setText(String.valueOf(dashboard.getOpenPositions()));
                binding.statPendingApprovals.statValue.setText(String.valueOf(dashboard.getPendingLeaveApprovals()));
            }

            @Override
            public void onFailure(Call<HrDashboardResponse> call, Throwable t) {
                hrLoaded = true;
                updateProgress();
                showHrUnavailable();
            }
        });
    }

    // A 403 here (missing FINANCIAL_REPORT_VIEW/EMPLOYEE_VIEW permission) is expected for some
    // owner accounts depending on their custom role config — each section fails independently
    // rather than blanking the whole screen.
    private void showFinanceUnavailable() {
        binding.statRevenue.getRoot().setVisibility(View.GONE);
        binding.statExpenses.getRoot().setVisibility(View.GONE);
        binding.statNetProfit.getRoot().setVisibility(View.GONE);
        binding.statCashCollected.getRoot().setVisibility(View.GONE);
        binding.statOutstanding.getRoot().setVisibility(View.GONE);
        binding.statOverdue.getRoot().setVisibility(View.GONE);
        binding.labelFinance.setText(R.string.error_finance_dashboard_unavailable);
    }

    private void showHrUnavailable() {
        binding.statTotalEmployees.getRoot().setVisibility(View.GONE);
        binding.statPresentToday.getRoot().setVisibility(View.GONE);
        binding.statOnLeaveToday.getRoot().setVisibility(View.GONE);
        binding.statAbsentToday.getRoot().setVisibility(View.GONE);
        binding.statOpenPositions.getRoot().setVisibility(View.GONE);
        binding.statPendingApprovals.getRoot().setVisibility(View.GONE);
        binding.labelHr.setText(R.string.error_hr_dashboard_unavailable);
    }

    private void updateProgress() {
        if (financeLoaded && hrLoaded) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }
}
