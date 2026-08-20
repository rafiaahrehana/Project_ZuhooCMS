package com.raf.zuhoo.ui.invoice;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.databinding.ActivityInvoiceListBinding;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.ui.common.CacheStamp;
import com.raf.zuhoo.ui.payment.PaymentReceiptListActivity;

import java.util.ArrayList;

public class InvoiceListActivity extends BottomNavActivity {

    private ActivityInvoiceListBinding binding;
    private InvoiceListViewModel viewModel;
    private InvoiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityInvoiceListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(InvoiceListViewModel.class);

        adapter = new InvoiceAdapter(new ArrayList<>(), this::openDetail);
        binding.invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.invoicesRecyclerView.setAdapter(adapter);

        binding.btnViewReceipts.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentReceiptListActivity.class)));

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from a payment should show the new balance.
        viewModel.start();
    }

    private void observeViewModel() {

        viewModel.items().observe(this, invoices -> {
            adapter.submitList(invoices);
            if (invoices.isEmpty()) {
                binding.stateView.showEmpty(R.drawable.ic_billing,
                        R.string.empty_invoices, R.string.empty_invoices_subtitle);
            } else {
                binding.stateView.showContent();
            }
        });

        viewModel.loading().observe(this, loading -> {
            if (loading && adapter.getItemCount() == 0) {
                binding.stateView.showLoading();
            }
        });

        viewModel.showingCached().observe(this, cached ->
                CacheStamp.bind(binding.cacheStamp, cached, viewModel.lastUpdated().getValue()));

        // Only fires when there's nothing cached to fall back on (see CachedListViewModel) — a
        // background refresh failing with content already on screen never reaches here.
        viewModel.error().observe(this, event -> {
            Integer messageRes = event.consume();
            if (messageRes != null) {
                binding.stateView.showError(messageRes, v -> viewModel.refresh());
            }
        });
    }

    private void openDetail(InvoiceSummary invoice) {
        Intent intent = new Intent(this, InvoiceDetailActivity.class);
        intent.putExtra(InvoiceDetailActivity.EXTRA_INVOICE_ID, invoice.getId());
        startActivity(intent);
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_billing;
    }
}
