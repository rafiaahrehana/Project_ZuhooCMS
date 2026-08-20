package com.raf.zuhoo.ui.invoice;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.InvoiceItem;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.data.repository.PaymentRepository;
import com.raf.zuhoo.databinding.ActivityInvoiceDetailBinding;
import com.raf.zuhoo.ui.common.StatusBadgeView;
import com.raf.zuhoo.ui.payment.PaymentActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import okhttp3.ResponseBody;

public class InvoiceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_INVOICE_ID = "extra_invoice_id";

    private ActivityInvoiceDetailBinding binding;
    private InvoiceDetailViewModel viewModel;
    private ActivityResultLauncher<Intent> paymentLauncher;

    private long invoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Money on screen — keep it out of screenshots and the recent-apps thumbnail.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        binding = ActivityInvoiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        invoiceId = getIntent().getLongExtra(EXTRA_INVOICE_ID, -1);

        if (invoiceId < 0) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(InvoiceDetailViewModel.class);

        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::onPaymentResult);

        binding.btnPayNow.setOnClickListener(v -> payNow());
        binding.btnDownloadPdf.setOnClickListener(v -> viewModel.downloadPdf());

        observeViewModel();

        viewModel.start(invoiceId);
    }

    private void observeViewModel() {

        viewModel.invoice().observe(this, this::bindInvoice);

        viewModel.loading().observe(this, loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.confirmState().observe(this, this::bindConfirmState);

        viewModel.error().observe(this, event -> {
            Integer messageRes = event.consume();
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.pdf().observe(this, event -> {
            ResponseBody body = event.consume();
            if (body != null) {
                savePdfAndOpen(body);
            }
        });
    }

    private void bindConfirmState(InvoiceDetailViewModel.ConfirmState state) {

        switch (state) {
            case CONFIRMING:
                binding.confirmingPaymentText.setText(R.string.payment_confirming);
                binding.confirmingPaymentText.setVisibility(View.VISIBLE);
                break;
            case CONFIRMED:
                binding.confirmingPaymentText.setVisibility(View.GONE);
                Toast.makeText(this, R.string.payment_success, Toast.LENGTH_LONG).show();
                break;
            case UNCONFIRMED:
                binding.confirmingPaymentText.setText(R.string.payment_not_yet_confirmed);
                binding.confirmingPaymentText.setVisibility(View.VISIBLE);
                break;
            default:
                binding.confirmingPaymentText.setVisibility(View.GONE);
                break;
        }
    }

    private void bindInvoice(InvoiceSummary invoice) {

        String currency = invoice.getCurrency() != null ? invoice.getCurrency() : "";

        binding.detailInvoiceNumber.setText(invoice.getInvoiceNumber());
        StatusBadgeView.bind(binding.detailStatusBadge,
                InvoiceStatusBadge.colorFor(this, invoice.getStatus()),
                InvoiceStatusBadge.labelFor(this, invoice.getStatus()));
        binding.detailServiceRequestTitle.setText(invoice.getServiceRequestTitle());
        binding.detailDates.setText(invoice.getInvoiceDate() + "  →  " + invoice.getDueDate());

        binding.itemsContainer.removeAllViews();
        for (InvoiceItem item : invoice.getItems()) {
            TextView row = new TextView(this);
            row.setTextAppearance(R.style.TextAppearance_Zuhoo_BodyMedium);
            row.setText(item.getDescription() + "  ×" + item.getQuantity()
                    + "  =  " + currency + " " + item.getLineTotal());
            row.setPadding(0, 0, 0, dpToPx(8));
            binding.itemsContainer.addView(row);
        }

        binding.detailSubtotal.setText(currency + " " + str(invoice.getSubtotal()));
        binding.detailTax.setText(currency + " " + str(invoice.getTaxAmount()));
        binding.detailTotal.setText(currency + " " + str(invoice.getTotalAmount()));
        binding.detailPaid.setText(currency + " " + str(invoice.getPaidAmount()));
        binding.detailBalance.setText(currency + " " + str(invoice.getBalanceAmount()));

        binding.detailNotes.setText(invoice.getNotes());
        binding.detailNotes.setVisibility(TextUtils.isEmpty(invoice.getNotes()) ? View.GONE : View.VISIBLE);

        boolean hasBalance = invoice.getBalanceAmount() != null && invoice.getBalanceAmount().signum() > 0;
        binding.btnPayNow.setVisibility(hasBalance ? View.VISIBLE : View.GONE);
    }

    private String str(BigDecimal value) {
        return value != null ? value.toPlainString() : "-";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void payNow() {

        InvoiceSummary invoice = viewModel.invoice().getValue();

        if (invoice == null || invoice.getBalanceAmount() == null) {
            return;
        }

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_TARGET_ID, invoiceId);
        intent.putExtra(PaymentActivity.EXTRA_AMOUNT, invoice.getBalanceAmount().toPlainString());
        intent.putExtra(PaymentActivity.EXTRA_PURPOSE, PaymentRepository.PURPOSE_INVOICE);
        paymentLauncher.launch(intent);
    }

    private void onPaymentResult(androidx.activity.result.ActivityResult result) {

        if (result.getResultCode() != RESULT_OK) {

            String status = result.getData() != null
                    ? result.getData().getStringExtra(PaymentActivity.EXTRA_GATEWAY_STATUS) : null;

            Toast.makeText(this,
                    status != null ? getString(R.string.payment_failed_with_status, status)
                            : getString(R.string.payment_failed),
                    Toast.LENGTH_LONG).show();

            // Still re-read the invoice — SSLCommerz's IPN is independent of the browser
            // redirect and may have settled it anyway.
            viewModel.reload();
            return;
        }

        viewModel.confirmPayment();
    }

    private void savePdfAndOpen(ResponseBody body) {
        try {
            openPdf(writePdfToCache(body));
            Toast.makeText(this, R.string.pdf_downloaded, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_pdf_download_failed, Toast.LENGTH_LONG).show();
        }
    }

    private File writePdfToCache(ResponseBody body) throws IOException {

        File dir = new File(getCacheDir(), "pdfs");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "invoice-" + invoiceId + ".pdf");

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(body.bytes());
        }

        return file;
    }

    private void openPdf(File file) {

        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_pdf_no_viewer, Toast.LENGTH_LONG).show();
        }
    }
}
