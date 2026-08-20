package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GlobalSearchResponse {

    @SerializedName("totalMatches")
    private long totalMatches;
    @SerializedName("results")
    private List<SearchResultItem> results;

    public long getTotalMatches() {
        return totalMatches;
    }

    public List<SearchResultItem> getResults() {
        return results;
    }
}
