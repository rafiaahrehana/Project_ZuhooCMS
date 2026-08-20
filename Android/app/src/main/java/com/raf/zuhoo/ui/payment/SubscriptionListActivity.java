package com.raf.zuhoo.ui.payment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.SecureScreen;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;
import com.raf.zuhoo.data.repository.PaymentRepository;
import com.raf.zuhoo.data.repository.SubscriptionRepository;
import com.raf.zuhoo.databinding.ActivitySubscriptionListBinding;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionListActivity extends AppCompatActivity implements SubscriptionAdapter.Listener {

    private ActivitySubscriptionListBinding binding;
    private SubscriptionRepository subscriptionRepository;
    private SubscriptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shows money / personal details - keep it out of screenshots and recents.
        SecureScreen.apply(this);

        binding = ActivitySubscriptionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        subscriptionRepository = new SubscriptionRepository(this);

        adapter = new SubscriptionAdapter(new ArrayList<>(), this);
        binding.subscriptionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.subscriptionsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubscriptions();
    }

    private void loadSubscriptions() {

        binding.stateView.showLoading();

        subscriptionRepository.getMySubscriptions(new Callback<PageResponse<SubscriptionSummary>>() {

            @Override
            public void onResponse(Call<PageResponse<SubscriptionSummary>> call,
                                   Response<PageResponse<SubscriptionSummary>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    // A refresh (e.g. returning to this screen after a payment) can fail with
                    // subscriptions already on screen from the previous load — the adapter isn't
                    // cleared on failure, so swapping to the state view's full error message
                    // here would wrongly hide content that's still valid. Keep the toast.
                    binding.stateView.showContent();
                    Toast.makeText(SubscriptionListActivity.this,
                            R.string.error_subscriptions_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                List<SubscriptionSummary> subscriptions = response.body().getContent();
                adapter.submitList(subscriptions);
                if (subscriptions.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_wallet,
                            R.string.empty_subscriptions, R.string.empty_subscriptions_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<SubscriptionSummary>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(SubscriptionListActivity.this,
                        R.string.error_subscriptions_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPayNow(SubscriptionSummary subscription) {

        if (subscription.getPricePaid() == null) {
            return;
        }

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_TARGET_ID, subscription.getId());
        intent.putExtra(PaymentActivity.EXTRA_AMOUNT, subscription.getPricePaid().toPlainString());
        intent.putExtra(PaymentActivity.EXTRA_PURPOSE, PaymentRepository.PURPOSE_PACKAGE_SUBSCRIPTION);
        startActivity(intent);
    }

    @Override
    public void onCancel(SubscriptionSummary subscription) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cancel_subscription_title)
                .setMessage(R.string.dialog_cancel_message)
                .setPositiveButton(R.string.action_yes_cancel, (dialog, which) -> cancelSubscription(subscription))
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void cancelSubscription(SubscriptionSummary subscription) {

        subscriptionRepository.cancelSubscription(subscription.getId(), null, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    UiErrors.show(SubscriptionListActivity.this, response, getString(R.string.error_cancel_subscription_failed));
                    return;
                }

                Toast.makeText(SubscriptionListActivity.this,
                        R.string.subscription_cancelled, Toast.LENGTH_SHORT).show();
                loadSubscriptions();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(SubscriptionListActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
