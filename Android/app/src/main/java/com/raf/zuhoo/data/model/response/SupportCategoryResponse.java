package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SupportCategoryResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("categoryName")
    private String categoryName;

    public Long getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    @Override
    public String toString() {
        return categoryName;
    }
}
