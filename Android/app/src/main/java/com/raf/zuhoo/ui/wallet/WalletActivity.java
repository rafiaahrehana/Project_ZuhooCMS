package com.raf.zuhoo.ui.wallet;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.WalletResponse;
import com.raf.zuhoo.data.model.response.WalletTransactionResponse;
import com.raf.zuhoo.data.repository.WalletRepository;
import com.raf.zuhoo.databinding.ActivityWalletBinding;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private WalletRepository repository;
    private WalletTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new WalletRepository(this);

        adapter = new WalletTransactionAdapter(new ArrayList<>());
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.transactionsRecyclerView.setAdapter(adapter);

        loadWallet();
        loadTransactions();
    }

    private void loadWallet() {

        repository.getWallet(new Callback<WalletResponse>() {

            @Override
            public void onResponse(Call<WalletResponse> call, Response<WalletResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(WalletActivity.this, R.string.error_wallet_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                WalletResponse wallet = response.body();
                binding.totalAvailableText.setText(wallet.getCurrency() + " " + wallet.getTotalAvailable());
                binding.balanceText.setText(wallet.getCurrency() + " " + wallet.getBalance());
                binding.creditBalanceText.setText(wallet.getCurrency() + " " + wallet.getCreditBalance());
            }

            @Override
            public void onFailure(Call<WalletResponse> call, Throwable t) {
                Toast.makeText(WalletActivity.this, R.string.error_wallet_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTransactions() {

        binding.stateView.showLoading();

        repository.getTransactions(new Callback<PageResponse<WalletTransactionResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<WalletTransactionResponse>> call,
                                   Response<PageResponse<WalletTransactionResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showError(R.string.error_wallet_load_failed, v -> loadTransactions());
                    return;
                }

                java.util.List<WalletTransactionResponse> transactions = response.body().getContent();
                adapter.submitList(transactions);
                if (transactions.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_wallet,
                            R.string.empty_wallet_transactions, R.string.empty_wallet_transactions_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<WalletTransactionResponse>> call, Throwable t) {
                binding.stateView.showError(R.string.error_wallet_load_failed, v -> loadTransactions());
            }
        });
    }
}
