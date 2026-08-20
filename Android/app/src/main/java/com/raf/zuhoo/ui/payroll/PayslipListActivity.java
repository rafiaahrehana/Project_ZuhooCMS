package com.raf.zuhoo.ui.payroll;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.PayrollResponse;
import com.raf.zuhoo.data.repository.PayrollRepository;
import com.raf.zuhoo.databinding.ActivityPayslipListBinding;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "My Payslips" — payroll has no bare "/my" endpoint (unlike leave/attendance), so this screen
 * first resolves the caller's own employeeId via /api/employees/me, then lists payroll by that
 * id. No ViewModel: a two-step, rarely-changing list like this doesn't carry its weight (same
 * reasoning as AttendanceLocationSettingsActivity).
 */
public class PayslipListActivity extends AppCompatActivity {

    private ActivityPayslipListBinding binding;
    private PayrollRepository repository;
    private PayslipAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPayslipListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PayrollRepository(this);

        adapter = new PayslipAdapter(new ArrayList<>(), this::downloadPdf);
        binding.payslipsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.payslipsRecyclerView.setAdapter(adapter);

        load();
    }

    private void load() {

        binding.stateView.showLoading();

        repository.getMyEmployeeProfile(new Callback<EmployeeResponse>() {

            @Override
            public void onResponse(Call<EmployeeResponse> call, Response<EmployeeResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    showLoadError();
                    return;
                }

                loadPayslips(response.body().getId());
            }

            @Override
            public void onFailure(Call<EmployeeResponse> call, Throwable t) {
                showLoadError();
            }
        });
    }

    private void loadPayslips(Long employeeId) {

        repository.getMyPayslips(employeeId, new Callback<PageResponse<PayrollResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<PayrollResponse>> call,
                                   Response<PageResponse<PayrollResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    showLoadError();
                    return;
                }

                java.util.List<PayrollResponse> payslips = response.body().getContent();
                adapter.submitList(payslips);
                if (payslips.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_document,
                            R.string.empty_payslips, R.string.empty_payslips_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<PayrollResponse>> call, Throwable t) {
                showLoadError();
            }
        });
    }

    private void downloadPdf(PayrollResponse payslip) {

        Toast.makeText(this, R.string.downloading_pdf, Toast.LENGTH_SHORT).show();

        repository.downloadPayslipPdf(payslip.getId(), new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PayslipListActivity.this, R.string.error_pdf_download_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                com.raf.zuhoo.ui.common.PdfOpener.writeAndOpen(
                        PayslipListActivity.this, response.body(), "payslip-" + payslip.getId() + ".pdf");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(PayslipListActivity.this, R.string.error_pdf_download_failed,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // load() only ever runs once per activity instance (from onCreate, no onResume refresh), so
    // a failure here can never be replacing content already on screen — safe to hand the whole
    // screen over to the error state rather than just toasting.
    private void showLoadError() {
        binding.stateView.showError(R.string.error_payslips_load_failed, v -> load());
    }
}
