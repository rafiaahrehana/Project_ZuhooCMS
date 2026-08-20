package com.raf.zuhoo.ui.payment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiConfig;
import com.raf.zuhoo.data.repository.PaymentRepository;
import com.raf.zuhoo.databinding.ActivityPaymentBinding;

import java.math.BigDecimal;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_ID = "extra_target_id";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_PURPOSE = "extra_purpose";

    // Set on a RESULT_CANCELED result: the raw GatewayTransactionStatus the redirect carried
    // (FAILED, CANCELLED, VALIDATION_FAILED), or null if it didn't carry one.
    public static final String EXTRA_GATEWAY_STATUS = "extra_gateway_status";

    private static final String STATUS_SUCCESS = "SUCCESS";

    private ActivityPaymentBinding binding;
    private PaymentRepository paymentRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Payment screen — never let this land in a screenshot or the recent-apps thumbnail.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long targetId = getIntent().getLongExtra(EXTRA_TARGET_ID, -1);
        String amountText = getIntent().getStringExtra(EXTRA_AMOUNT);
        String purpose = getIntent().getStringExtra(EXTRA_PURPOSE);

        if (targetId < 0 || amountText == null) {
            finish();
            return;
        }

        if (purpose == null) {
            purpose = PaymentRepository.PURPOSE_INVOICE;
        }

        paymentRepository = new PaymentRepository(this);

        setUpWebView();
        initiatePayment(purpose, targetId, new BigDecimal(amountText));
    }

    private void setUpWebView() {

        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        binding.webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, @NonNull WebResourceRequest request) {

                Uri uri = request.getUrl();

                if (uri.toString().contains("/payment-result")) {
                    finishWithGatewayResult(uri);
                    return true;
                }

                if (!isAllowedHost(uri.getHost())) {
                    return true;
                }

                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    // The redirect carries the backend's own GatewayTransactionStatus enum name, so the
    // "success" callback path can still report VALIDATION_FAILED — server-to-server validation
    // runs after the browser has already been sent onward. Only SUCCESS is worth waiting on;
    // everything else should stop the caller polling for a settlement that isn't coming.
    private void finishWithGatewayResult(Uri uri) {

        String status = uri.getQueryParameter("status");

        if (STATUS_SUCCESS.equals(status)) {
            // Still only a hint — the caller re-reads the invoice, which is the real source of
            // truth, because the IPN may not have landed yet.
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_GATEWAY_STATUS, status));
        }

        finish();
    }

    private boolean isAllowedHost(@Nullable String host) {

        if (host == null) {
            return false;
        }

        // The gateway itself, plus whatever host the API is on for this flavor (the backend's
        // own callback URLs live there). The frontend's /payment-result target doesn't need
        // listing — it's intercepted above before it ever loads.
        return host.endsWith("sslcommerz.com") || host.equals(apiHost());
    }

    private String apiHost() {
        String host = Uri.parse(ApiConfig.baseUrl()).getHost();
        return host != null ? host : "";
    }

    private void initiatePayment(String purpose, long targetId, BigDecimal amount) {

        paymentRepository.initiate(purpose, targetId, amount,
                new Callback<Map<String, String>>() {

            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {

                if (!response.isSuccessful() || response.body() == null
                        || response.body().get("gatewayUrl") == null) {
                    Toast.makeText(PaymentActivity.this,
                            R.string.error_payment_initiate_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                binding.webView.loadUrl(response.body().get("gatewayUrl"));
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(PaymentActivity.this,
                        R.string.error_payment_initiate_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
