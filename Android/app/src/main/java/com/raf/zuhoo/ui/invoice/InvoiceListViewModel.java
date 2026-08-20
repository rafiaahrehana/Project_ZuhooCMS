package com.raf.zuhoo.ui.invoice;

import android.app.Application;

import androidx.annotation.NonNull;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.InvoiceRepository;
import com.raf.zuhoo.ui.common.CachedListViewModel;

import retrofit2.Callback;

public class InvoiceListViewModel extends CachedListViewModel<InvoiceSummary> {

    private final InvoiceRepository repository;

    public InvoiceListViewModel(@NonNull Application application) {
        super(application, ListCache.INVOICES, InvoiceSummary.class);
        repository = new InvoiceRepository(application);
    }

    @Override
    protected void fetch(Callback<PageResponse<InvoiceSummary>> callback) {
        repository.getMyInvoices(callback);
    }

    @Override
    protected Long idOf(InvoiceSummary item) {
        return item.getId();
    }

    @Override
    protected int loadErrorRes() {
        return R.string.error_invoices_load_failed;
    }
}
