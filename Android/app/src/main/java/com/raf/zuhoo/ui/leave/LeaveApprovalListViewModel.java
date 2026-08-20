package com.raf.zuhoo.ui.leave;

import android.app.Application;

import androidx.annotation.NonNull;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.LeaveRequestRepository;
import com.raf.zuhoo.ui.common.CachedListViewModel;

import retrofit2.Callback;

public class LeaveApprovalListViewModel extends CachedListViewModel<LeaveRequestResponse> {

    private final LeaveRequestRepository repository;

    public LeaveApprovalListViewModel(@NonNull Application application) {
        super(application, ListCache.LEAVE_APPROVALS, LeaveRequestResponse.class);
        repository = new LeaveRequestRepository(application);
    }

    @Override
    protected void fetch(Callback<PageResponse<LeaveRequestResponse>> callback) {
        repository.getPendingLeaveRequests(callback);
    }

    @Override
    protected Long idOf(LeaveRequestResponse item) {
        return item.getId();
    }

    @Override
    protected int loadErrorRes() {
        return R.string.error_leave_approvals_load_failed;
    }
}
