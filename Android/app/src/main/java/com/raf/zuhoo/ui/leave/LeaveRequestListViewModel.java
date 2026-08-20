package com.raf.zuhoo.ui.leave;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.LeaveBalanceResponse;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.LeaveRequestRepository;
import com.raf.zuhoo.ui.common.CachedListViewModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaveRequestListViewModel extends CachedListViewModel<LeaveRequestResponse> {

    private final LeaveRequestRepository repository;

    // Balances aren't paged/cached like the request list — a plain live fetch alongside it,
    // same reasoning as AttendanceLocationSettingsActivity not using CachedListViewModel for a
    // single small settings object.
    private final MutableLiveData<List<LeaveBalanceResponse>> balances = new MutableLiveData<>();

    public LeaveRequestListViewModel(@NonNull Application application) {
        super(application, ListCache.LEAVE_REQUESTS, LeaveRequestResponse.class);
        repository = new LeaveRequestRepository(application);
    }

    @Override
    protected void fetch(Callback<PageResponse<LeaveRequestResponse>> callback) {
        repository.getMyLeaveRequests(callback);
    }

    @Override
    protected Long idOf(LeaveRequestResponse item) {
        return item.getId();
    }

    @Override
    protected int loadErrorRes() {
        return R.string.error_leave_requests_load_failed;
    }

    public LiveData<List<LeaveBalanceResponse>> balances() {
        return balances;
    }

    @Override
    public void start() {
        super.start();
        loadBalances();
    }

    public void loadBalances() {
        repository.getMyBalances(new Callback<List<LeaveBalanceResponse>>() {

            @Override
            public void onResponse(Call<List<LeaveBalanceResponse>> call, Response<List<LeaveBalanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    balances.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<LeaveBalanceResponse>> call, Throwable t) {
                // Balances are a secondary card on this screen — the request list's own error
                // handling is enough; failing quietly here just leaves the balance row empty.
            }
        });
    }

    public void refreshAll() {
        refresh();
        loadBalances();
    }
}
