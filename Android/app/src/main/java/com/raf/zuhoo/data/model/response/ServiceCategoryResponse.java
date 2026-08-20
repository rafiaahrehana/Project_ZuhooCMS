package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ServiceCategoryResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("sortOrder")
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
