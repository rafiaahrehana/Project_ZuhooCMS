package com.raf.zuhoo.ui.servicerequest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.data.repository.ServiceRequestRepository;
import com.raf.zuhoo.databinding.ActivityStaffServiceRequestListBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffServiceRequestListActivity extends BottomNavActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_ALL = "ALL";
    public static final String MODE_ASSIGNED_TO_ME = "ASSIGNED_TO_ME";

    private ActivityStaffServiceRequestListBinding binding;
    private ServiceRequestRepository serviceRequestRepository;
    private ServiceRequestAdapter adapter;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStaffServiceRequestListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_ALL;
        }

        binding.screenTitle.setText(MODE_ASSIGNED_TO_ME.equals(mode)
                ? R.string.action_assigned_requests : R.string.action_all_requests);

        serviceRequestRepository = new ServiceRequestRepository(this);

        adapter = new ServiceRequestAdapter(new ArrayList<>(), this::openDetail);
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {

        binding.stateView.showLoading();

        Callback<PageResponse<ServiceRequestSummary>> callback =
                new Callback<PageResponse<ServiceRequestSummary>>() {

            @Override
            public void onResponse(Call<PageResponse<ServiceRequestSummary>> call,
                                   Response<PageResponse<ServiceRequestSummary>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    // Content (if any) from a previous successful load stays on screen — this
                    // toast can fire on a background refresh, not only when the list is empty.
                    binding.stateView.showContent();
                    Toast.makeText(StaffServiceRequestListActivity.this,
                            R.string.error_requests_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                List<ServiceRequestSummary> requests = response.body().getContent();
                adapter.submitList(requests);
                if (requests.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_requests,
                            R.string.empty_requests, R.string.empty_requests_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ServiceRequestSummary>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(StaffServiceRequestListActivity.this,
                        R.string.error_requests_load_failed, Toast.LENGTH_LONG).show();
            }
        };

        if (MODE_ASSIGNED_TO_ME.equals(mode)) {
            serviceRequestRepository.getAssignedToMe(callback);
        } else {
            serviceRequestRepository.getAllServiceRequests(null, callback);
        }
    }

    private void openDetail(ServiceRequestSummary request) {
        Intent intent = new Intent(this, ServiceRequestDetailActivity.class);
        intent.putExtra(ServiceRequestDetailActivity.EXTRA_REQUEST_ID, request.getId());
        startActivity(intent);
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_requests;
    }
}
