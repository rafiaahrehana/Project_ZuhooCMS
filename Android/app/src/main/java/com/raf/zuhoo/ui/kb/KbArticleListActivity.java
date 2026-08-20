package com.raf.zuhoo.ui.kb;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.KbArticleResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.KbArticleRepository;
import com.raf.zuhoo.databinding.ActivityKbArticleListBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KbArticleListActivity extends AppCompatActivity {

    private ActivityKbArticleListBinding binding;
    private KbArticleRepository kbArticleRepository;
    private KbArticleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityKbArticleListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        kbArticleRepository = new KbArticleRepository(this);

        adapter = new KbArticleAdapter(new ArrayList<>(), this::openArticle);
        binding.articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.articlesRecyclerView.setAdapter(adapter);

        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadArticles();
                return true;
            }
            return false;
        });

        loadArticles();
    }

    private void loadArticles() {

        binding.stateView.showLoading();

        String keyword = binding.searchEditText.getText() == null
                ? "" : binding.searchEditText.getText().toString().trim();

        kbArticleRepository.getArticles(TextUtils.isEmpty(keyword) ? null : keyword,
                new Callback<PageResponse<KbArticleResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<KbArticleResponse>> call,
                                   Response<PageResponse<KbArticleResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(KbArticleListActivity.this,
                            R.string.error_articles_load_failed, Toast.LENGTH_LONG).show();
                    if (adapter.getItemCount() == 0) {
                        binding.stateView.showContent();
                    }
                    return;
                }

                List<KbArticleResponse> articles = response.body().getContent();
                adapter.submitList(articles);
                if (articles.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_document,
                            R.string.empty_articles, R.string.empty_articles_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<KbArticleResponse>> call, Throwable t) {
                Toast.makeText(KbArticleListActivity.this,
                        R.string.error_articles_load_failed, Toast.LENGTH_LONG).show();
                if (adapter.getItemCount() == 0) {
                    binding.stateView.showContent();
                }
            }
        });
    }

    private void openArticle(KbArticleResponse article) {
        Intent intent = new Intent(this, KbArticleDetailActivity.class);
        intent.putExtra(KbArticleDetailActivity.EXTRA_ARTICLE_ID, article.getId());
        startActivity(intent);
    }
}
