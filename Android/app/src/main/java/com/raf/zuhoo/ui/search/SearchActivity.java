package com.raf.zuhoo.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.GlobalSearchResponse;
import com.raf.zuhoo.data.repository.SearchRepository;
import com.raf.zuhoo.databinding.ActivitySearchBinding;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Flat cross-module search — leads, clients, opportunities, service requests, tickets, invoices
 * all come back in one list, tagged by type. No deep-linking into each result type yet (the
 * backend returns a web route in `link`, unused here); this is a "find it, then go look it up in
 * the right screen yourself" tool for now.
 */
public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private SearchRepository repository;
    private SearchResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SearchRepository(this);

        adapter = new SearchResultAdapter(new ArrayList<>());
        binding.resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.resultsRecyclerView.setAdapter(adapter);

        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                runSearch();
                return true;
            }
            return false;
        });

        // Nothing searched yet — distinct from "searched, found nothing" below, same
        // ic_search-vs-ic_inbox + copy split used on other search-like screens.
        binding.stateView.showEmpty(R.drawable.ic_search,
                R.string.empty_search_prompt_title, R.string.empty_search_prompt_subtitle);
    }

    private void runSearch() {

        String query = binding.searchEditText.getText() == null
                ? "" : binding.searchEditText.getText().toString().trim();

        if (TextUtils.isEmpty(query)) {
            return;
        }

        binding.stateView.showLoading();

        repository.search(query, new Callback<GlobalSearchResponse>() {

            @Override
            public void onResponse(Call<GlobalSearchResponse> call, Response<GlobalSearchResponse> response) {

                if (!response.isSuccessful() || response.body() == null || response.body().getResults() == null) {
                    binding.stateView.showContent();
                    Toast.makeText(SearchActivity.this, R.string.error_search_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                java.util.List<com.raf.zuhoo.data.model.response.SearchResultItem> results = response.body().getResults();
                adapter.submitList(results);

                if (results.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_inbox,
                            R.string.empty_search_results, R.string.empty_search_results_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<GlobalSearchResponse> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(SearchActivity.this, R.string.error_search_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}
