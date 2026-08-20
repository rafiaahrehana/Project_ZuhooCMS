package com.businessos.modules.search;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class GlobalSearchResponse {
    private String query;
    private List<SearchResultItem> results = new ArrayList<>();
    private long totalMatches;
}
