package com.raf.zuhoo.ui.servicerequest;

import android.app.Application;

import androidx.annotation.NonNull;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.data.repository.ServiceRequestRepository;
import com.raf.zuhoo.ui.common.CachedListViewModel;

import retrofit2.Callback;

public class ServiceRequestListViewModel extends CachedListViewModel<ServiceRequestSummary> {

    private final ServiceRequestRepository repository;

    public ServiceRequestListViewModel(@NonNull Application application) {
        super(application, ListCache.SERVICE_REQUESTS, ServiceRequestSummary.class);
        repository = new ServiceRequestRepository(application);
    }

    @Override
    protected void fetch(Callback<PageResponse<ServiceRequestSummary>> callback) {
        repository.getMyServiceRequests(callback);
    }

    @Override
    protected Long idOf(ServiceRequestSummary item) {
        return item.getId();
    }

    @Override
    protected int loadErrorRes() {
        return R.string.error_requests_load_failed;
    }
}
