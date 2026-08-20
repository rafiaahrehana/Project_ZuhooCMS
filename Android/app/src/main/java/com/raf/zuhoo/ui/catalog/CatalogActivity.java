package com.raf.zuhoo.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.data.model.response.CompanyServiceResponse;
import com.raf.zuhoo.data.model.response.ServiceCategoryResponse;
import com.raf.zuhoo.data.repository.CatalogRepository;
import com.raf.zuhoo.databinding.ActivityCatalogBinding;
import com.raf.zuhoo.ui.servicerequest.CreateServiceRequestActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogActivity extends BottomNavActivity {

    private ActivityCatalogBinding binding;
    private CatalogRepository catalogRepository;
    private CatalogAdapter adapter;

    private List<ServiceCategoryResponse> categories;
    private List<CompanyServiceResponse> services;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCatalogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        catalogRepository = new CatalogRepository(this);

        adapter = new CatalogAdapter(new ArrayList<>(), this::requestService);
        binding.catalogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.catalogRecyclerView.setAdapter(adapter);

        loadCatalog();
    }

    private void loadCatalog() {

        binding.stateView.showLoading();

        catalogRepository.getServiceCategories(new Callback<List<ServiceCategoryResponse>>() {

            @Override
            public void onResponse(Call<List<ServiceCategoryResponse>> call,
                                   Response<List<ServiceCategoryResponse>> response) {
                categories = response.isSuccessful() && response.body() != null
                        ? response.body() : Collections.emptyList();
                tryBuildRows();
            }

            @Override
            public void onFailure(Call<List<ServiceCategoryResponse>> call, Throwable t) {
                categories = Collections.emptyList();
                tryBuildRows();
            }
        });

        catalogRepository.getActiveServices(new Callback<List<CompanyServiceResponse>>() {

            @Override
            public void onResponse(Call<List<CompanyServiceResponse>> call,
                                   Response<List<CompanyServiceResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    services = Collections.emptyList();
                    Toast.makeText(CatalogActivity.this,
                            R.string.error_catalog_load_failed, Toast.LENGTH_LONG).show();
                } else {
                    services = response.body();
                }
                tryBuildRows();
            }

            @Override
            public void onFailure(Call<List<CompanyServiceResponse>> call, Throwable t) {
                services = Collections.emptyList();
                Toast.makeText(CatalogActivity.this,
                        R.string.error_catalog_load_failed, Toast.LENGTH_LONG).show();
                tryBuildRows();
            }
        });
    }

    private void tryBuildRows() {

        if (categories == null || services == null) {
            return;
        }

        List<ServiceCategoryResponse> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories, Comparator.comparingInt(ServiceCategoryResponse::getSortOrder));

        List<CatalogRow> rows = new ArrayList<>();
        Set<Long> matchedServiceIds = new HashSet<>();

        for (ServiceCategoryResponse category : sortedCategories) {

            List<CompanyServiceResponse> inCategory = new ArrayList<>();
            for (CompanyServiceResponse service : services) {
                if (category.getId().equals(service.getCategoryId())) {
                    inCategory.add(service);
                    matchedServiceIds.add(service.getId());
                }
            }

            if (!inCategory.isEmpty()) {
                rows.add(CatalogRow.header(category.getName()));
                for (CompanyServiceResponse service : inCategory) {
                    rows.add(CatalogRow.service(service));
                }
            }
        }

        List<CompanyServiceResponse> uncategorized = new ArrayList<>();
        for (CompanyServiceResponse service : services) {
            if (!matchedServiceIds.contains(service.getId())) {
                uncategorized.add(service);
            }
        }

        if (!uncategorized.isEmpty()) {
            rows.add(CatalogRow.header(getString(R.string.catalog_other_category)));
            for (CompanyServiceResponse service : uncategorized) {
                rows.add(CatalogRow.service(service));
            }
        }

        adapter.submitList(rows);
        if (rows.isEmpty()) {
            binding.stateView.showEmpty(R.drawable.ic_catalog,
                    R.string.empty_catalog, R.string.empty_catalog_subtitle);
        } else {
            binding.stateView.showContent();
        }
    }

    private void requestService(CompanyServiceResponse service) {
        Intent intent = new Intent(this, CreateServiceRequestActivity.class);
        intent.putExtra(CreateServiceRequestActivity.EXTRA_PRESELECTED_SERVICE_ID, service.getId());
        startActivity(intent);
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_catalog;
    }
}
