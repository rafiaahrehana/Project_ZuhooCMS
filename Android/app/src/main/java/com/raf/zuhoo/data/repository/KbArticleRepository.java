package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.KbArticleResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class KbArticleRepository {

    private final ApiService apiService;

    public KbArticleRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getArticles(String keyword, Callback<PageResponse<KbArticleResponse>> callback) {
        apiService.getKbArticles(keyword).enqueue(callback);
    }

    public void getArticle(Long id, Callback<KbArticleResponse> callback) {
        apiService.getKbArticle(id).enqueue(callback);
    }

    public void markHelpful(Long id, Callback<ResponseBody> callback) {
        apiService.markKbArticleHelpful(id).enqueue(callback);
    }
}
