package com.raf.zuhoo.ui.payment;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.SecureScreen;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.PaymentReceiptSummary;
import com.raf.zuhoo.data.repository.PaymentReceiptRepository;
import com.raf.zuhoo.databinding.ActivityPaymentReceiptListBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentReceiptListActivity extends AppCompatActivity {

    private ActivityPaymentReceiptListBinding binding;
    private PaymentReceiptRepository paymentReceiptRepository;
    private PaymentReceiptAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shows money / personal details - keep it out of screenshots and recents.
        SecureScreen.apply(this);

        binding = ActivityPaymentReceiptListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        paymentReceiptRepository = new PaymentReceiptRepository(this);

        adapter = new PaymentReceiptAdapter(new ArrayList<>());
        binding.receiptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.receiptsRecyclerView.setAdapter(adapter);

        loadReceipts();
    }

    private void loadReceipts() {

        binding.stateView.showLoading();

        paymentReceiptRepository.getMyReceipts(new Callback<PageResponse<PaymentReceiptSummary>>() {

            @Override
            public void onResponse(Call<PageResponse<PaymentReceiptSummary>> call,
                                   Response<PageResponse<PaymentReceiptSummary>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showError(R.string.error_receipts_load_failed,
                            v -> loadReceipts());
                    return;
                }

                List<PaymentReceiptSummary> receipts = response.body().getContent();
                adapter.submitList(receipts);
                if (receipts.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_receipt,
                            R.string.empty_receipts, R.string.empty_receipts_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<PaymentReceiptSummary>> call, Throwable t) {
                binding.stateView.showError(R.string.error_receipts_load_failed, v -> loadReceipts());
            }
        });
    }
}
