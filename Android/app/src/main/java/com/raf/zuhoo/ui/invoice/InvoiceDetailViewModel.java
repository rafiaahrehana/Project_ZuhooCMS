package com.raf.zuhoo.ui.invoice;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.data.repository.InvoiceRepository;
import com.raf.zuhoo.ui.common.Event;

import java.math.BigDecimal;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Holds the invoice and the payment-confirmation poll across configuration changes. Previously
// both lived in the Activity, so a rotation mid-poll either lost the result or wrote into a
// destroyed view binding.
public class InvoiceDetailViewModel extends AndroidViewModel {

    // ~15s of patience in total, spread out rather than two quick polls two seconds apart —
    // the IPN commonly lands after the browser redirect, not before it.
    private static final long[] CONFIRM_BACKOFF_MS = {1000, 2000, 4000, 8000};

    public enum ConfirmState { IDLE, CONFIRMING, CONFIRMED, UNCONFIRMED }

    private final InvoiceRepository invoiceRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<InvoiceSummary> invoice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ConfirmState> confirmState = new MutableLiveData<>(ConfirmState.IDLE);
    private final MutableLiveData<Event<Integer>> error = new MutableLiveData<>();
    private final MutableLiveData<Event<ResponseBody>> pdf = new MutableLiveData<>();

    private long invoiceId = -1;
    private BigDecimal balanceBeforePayment;
    private Runnable pendingPoll;

    public InvoiceDetailViewModel(@NonNull Application application) {
        super(application);
        invoiceRepository = new InvoiceRepository(application);
    }

    public LiveData<InvoiceSummary> invoice() {
        return invoice;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<ConfirmState> confirmState() {
        return confirmState;
    }

    public LiveData<Event<Integer>> error() {
        return error;
    }

    public LiveData<Event<ResponseBody>> pdf() {
        return pdf;
    }

    // Called from onCreate every time, including after a rotation — only the first call fetches.
    public void start(long id) {

        if (invoiceId == id && invoice.getValue() != null) {
            return;
        }

        invoiceId = id;
        load();
    }

    public void reload() {
        load();
    }

    private void load() {

        loading.setValue(true);

        invoiceRepository.getInvoice(invoiceId, new Callback<InvoiceSummary>() {

            @Override
            public void onResponse(Call<InvoiceSummary> call, Response<InvoiceSummary> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(new Event<>(R.string.error_invoice_load_failed));
                    return;
                }

                invoice.setValue(response.body());
            }

            @Override
            public void onFailure(Call<InvoiceSummary> call, Throwable t) {
                loading.setValue(false);
                error.setValue(new Event<>(R.string.error_invoice_load_failed));
            }
        });
    }

    // The gateway redirect is not proof of payment: the backend re-validates server-to-server and
    // is also driven by an IPN callback that can land seconds after the browser comes back. So we
    // poll the invoice itself — the actual source of truth — and back off between attempts.
    public void confirmPayment() {
        InvoiceSummary current = invoice.getValue();
        balanceBeforePayment = current != null ? current.getBalanceAmount() : null;
        confirmState.setValue(ConfirmState.CONFIRMING);
        poll(0);
    }

    private void poll(int attempt) {

        invoiceRepository.getInvoice(invoiceId, new Callback<InvoiceSummary>() {

            @Override
            public void onResponse(Call<InvoiceSummary> call, Response<InvoiceSummary> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    scheduleOrGiveUp(attempt);
                    return;
                }

                invoice.setValue(response.body());

                if (settled(response.body())) {
                    confirmState.setValue(ConfirmState.CONFIRMED);
                    return;
                }

                scheduleOrGiveUp(attempt);
            }

            @Override
            public void onFailure(Call<InvoiceSummary> call, Throwable t) {
                scheduleOrGiveUp(attempt);
            }
        });
    }

    private boolean settled(InvoiceSummary latest) {
        return balanceBeforePayment == null
                || latest.getBalanceAmount() == null
                || balanceBeforePayment.compareTo(latest.getBalanceAmount()) != 0;
    }

    private void scheduleOrGiveUp(int attempt) {

        if (attempt >= CONFIRM_BACKOFF_MS.length) {
            // Not a failed payment — an unconfirmed one. Reporting "failed" here would be a lie
            // when the IPN is simply still in flight.
            confirmState.setValue(ConfirmState.UNCONFIRMED);
            return;
        }

        pendingPoll = () -> poll(attempt + 1);
        handler.postDelayed(pendingPoll, CONFIRM_BACKOFF_MS[attempt]);
    }

    public void downloadPdf() {

        invoiceRepository.downloadPdf(invoiceId, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(new Event<>(R.string.error_pdf_download_failed));
                    return;
                }

                pdf.setValue(new Event<>(response.body()));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                error.setValue(new Event<>(R.string.error_pdf_download_failed));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingPoll != null) {
            handler.removeCallbacks(pendingPoll);
        }
    }
}
