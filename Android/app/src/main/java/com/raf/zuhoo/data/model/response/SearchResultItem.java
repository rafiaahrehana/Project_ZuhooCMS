package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class SearchResultItem {

    @SerializedName("type")
    private String type;
    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("subtitle")
    private String subtitle;

    public String getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
