package com.arxivradar.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MetaResponse {

    private long totalCount;
    private LocalDateTime lastFetchedAt;
    private List<String> categories;

    public MetaResponse(long totalCount, LocalDateTime lastFetchedAt, List<String> categories) {
        this.totalCount = totalCount;
        this.lastFetchedAt = lastFetchedAt;
        this.categories = categories;
    }

    public long getTotalCount() { return totalCount; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public List<String> getCategories() { return categories; }
}
