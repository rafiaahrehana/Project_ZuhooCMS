package com.raf.zuhoo.ui.directory;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.EmployeeRepository;
import com.raf.zuhoo.databinding.ActivityDirectoryBinding;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DirectoryActivity extends AppCompatActivity {

    private ActivityDirectoryBinding binding;
    private EmployeeRepository repository;
    private DirectoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDirectoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmployeeRepository(this);

        adapter = new DirectoryAdapter(new ArrayList<>());
        binding.employeesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.employeesRecyclerView.setAdapter(adapter);

        load();
    }

    private void load() {

        binding.stateView.showLoading();

        repository.getEmployees(new Callback<PageResponse<EmployeeResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<EmployeeResponse>> call,
                                   Response<PageResponse<EmployeeResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showError(R.string.error_directory_load_failed, v -> load());
                    return;
                }

                java.util.List<EmployeeResponse> employees = response.body().getContent();
                adapter.submitList(employees);
                if (employees.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_inbox,
                            R.string.empty_directory, R.string.empty_directory_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<EmployeeResponse>> call, Throwable t) {
                binding.stateView.showError(R.string.error_directory_load_failed, v -> load());
            }
        });
    }
}
