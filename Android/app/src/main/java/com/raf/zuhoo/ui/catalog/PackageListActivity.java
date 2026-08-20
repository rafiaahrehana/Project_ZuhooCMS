package com.raf.zuhoo.ui.catalog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.api.ApiErrors;
import com.raf.zuhoo.ui.common.UiErrors;
import com.raf.zuhoo.data.model.response.ServicePackageResponse;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;
import com.raf.zuhoo.data.repository.PackageRepository;
import com.raf.zuhoo.databinding.ActivityPackageListBinding;
import com.raf.zuhoo.ui.payment.SubscriptionListActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PackageListActivity extends AppCompatActivity {

    private ActivityPackageListBinding binding;
    private PackageRepository packageRepository;
    private PackageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPackageListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        packageRepository = new PackageRepository(this);

        adapter = new PackageAdapter(new ArrayList<>(), this::confirmSubscribe);
        binding.packagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.packagesRecyclerView.setAdapter(adapter);

        binding.btnViewSubscriptions.setOnClickListener(v ->
                startActivity(new Intent(this, SubscriptionListActivity.class)));

        loadPackages();
    }

    private void loadPackages() {

        binding.stateView.showLoading();

        packageRepository.getActivePackages(new Callback<List<ServicePackageResponse>>() {

            @Override
            public void onResponse(Call<List<ServicePackageResponse>> call,
                                   Response<List<ServicePackageResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showContent();
                    Toast.makeText(PackageListActivity.this,
                            R.string.error_packages_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                List<ServicePackageResponse> packages = response.body();
                adapter.submitList(packages);
                if (packages.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_document,
                            R.string.empty_packages, R.string.empty_packages_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<List<ServicePackageResponse>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(PackageListActivity.this,
                        R.string.error_packages_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmSubscribe(ServicePackageResponse servicePackage) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_subscribe_title)
                .setMessage(getString(R.string.dialog_subscribe_message, servicePackage.getName()))
                .setPositiveButton(R.string.action_subscribe, (dialog, which) -> subscribe(servicePackage))
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void subscribe(ServicePackageResponse servicePackage) {

        packageRepository.subscribe(servicePackage.getId(), null, new Callback<SubscriptionSummary>() {

            @Override
            public void onResponse(Call<SubscriptionSummary> call, Response<SubscriptionSummary> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    UiErrors.show(PackageListActivity.this, response, getString(R.string.error_subscribe_failed));
                    return;
                }

                Toast.makeText(PackageListActivity.this,
                        R.string.subscribed_pending_payment, Toast.LENGTH_LONG).show();
                startActivity(new Intent(PackageListActivity.this, SubscriptionListActivity.class));
            }

            @Override
            public void onFailure(Call<SubscriptionSummary> call, Throwable t) {
                Toast.makeText(PackageListActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
