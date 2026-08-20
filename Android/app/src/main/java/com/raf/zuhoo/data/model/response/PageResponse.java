package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

// Mirrors the shape of a Spring Data Page<T> JSON body — only the two fields this app reads.
public class PageResponse<T> {

    @SerializedName("content")
    private List<T> content;
    @SerializedName("totalElements")
    private long totalElements;

    public List<T> getContent() {
        return content != null ? content : Collections.emptyList();
    }

    public long getTotalElements() {
        return totalElements;
    }
}
