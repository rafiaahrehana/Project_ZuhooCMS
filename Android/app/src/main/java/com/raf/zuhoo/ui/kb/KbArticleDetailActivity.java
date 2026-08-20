package com.raf.zuhoo.ui.kb;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.KbArticleResponse;
import com.raf.zuhoo.data.repository.KbArticleRepository;
import com.raf.zuhoo.databinding.ActivityKbArticleDetailBinding;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KbArticleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE_ID = "extra_article_id";

    private ActivityKbArticleDetailBinding binding;
    private KbArticleRepository kbArticleRepository;
    private long articleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityKbArticleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        articleId = getIntent().getLongExtra(EXTRA_ARTICLE_ID, -1);

        if (articleId < 0) {
            finish();
            return;
        }

        kbArticleRepository = new KbArticleRepository(this);

        binding.btnMarkHelpful.setOnClickListener(v -> markHelpful());

        loadArticle();
    }

    private void loadArticle() {

        binding.progressBar.setVisibility(View.VISIBLE);

        kbArticleRepository.getArticle(articleId, new Callback<KbArticleResponse>() {

            @Override
            public void onResponse(Call<KbArticleResponse> call, Response<KbArticleResponse> response) {

                binding.progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(KbArticleDetailActivity.this,
                            R.string.error_articles_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                KbArticleResponse article = response.body();
                binding.detailTitle.setText(article.getTitle());
                binding.detailCategory.setText(article.getCategoryName());
                binding.detailContent.setText(article.getContent());
            }

            @Override
            public void onFailure(Call<KbArticleResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(KbArticleDetailActivity.this,
                        R.string.error_articles_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void markHelpful() {

        binding.btnMarkHelpful.setEnabled(false);

        kbArticleRepository.markHelpful(articleId, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(KbArticleDetailActivity.this,
                        R.string.marked_helpful, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                binding.btnMarkHelpful.setEnabled(true);
                Toast.makeText(KbArticleDetailActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
