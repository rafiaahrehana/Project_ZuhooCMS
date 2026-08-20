package com.raf.zuhoo.ui.servicerequest;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;
import com.raf.zuhoo.databinding.ActivityServiceRequestListBinding;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.ui.common.CacheStamp;

import java.util.ArrayList;

public class ServiceRequestListActivity extends BottomNavActivity {

    private ActivityServiceRequestListBinding binding;
    private ServiceRequestListViewModel viewModel;
    private ServiceRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityServiceRequestListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ServiceRequestListViewModel.class);

        adapter = new ServiceRequestAdapter(new ArrayList<>(), this::openDetail);
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.requestsRecyclerView.setAdapter(adapter);

        binding.btnNewRequest.setOnClickListener(v ->
                startActivity(new Intent(this, CreateServiceRequestActivity.class)));

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-entering after creating or cancelling a request should show the new state; start()
        // renders the cache once and refreshes on every visit.
        viewModel.start();
    }

    private void observeViewModel() {

        viewModel.items().observe(this, requests -> {
            adapter.submitList(requests);
            if (requests.isEmpty()) {
                binding.stateView.showEmpty(R.drawable.ic_requests,
                        R.string.empty_requests, R.string.empty_requests_subtitle);
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
