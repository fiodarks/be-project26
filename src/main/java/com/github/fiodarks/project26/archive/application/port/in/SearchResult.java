package com.github.fiodarks.project26.archive.application.port.in;

import java.util.List;
import java.util.Objects;

public record SearchResult<T>(List<T> items, long totalItems, int page, int size) {
    public SearchResult {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be >= 0");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
    }
}

