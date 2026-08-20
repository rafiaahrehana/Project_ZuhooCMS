package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class KbArticleResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("summary")
    private String summary;
    @SerializedName("content")
    private String content;
    @SerializedName("categoryName")
    private String categoryName;
    @SerializedName("viewCount")
    private long viewCount;
    @SerializedName("helpfulCount")
    private long helpfulCount;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getHelpfulCount() {
        return helpfulCount;
    }
}
