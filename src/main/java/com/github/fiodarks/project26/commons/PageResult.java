package com.github.fiodarks.project26.commons;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must be >= 0");
        }
    }
}

