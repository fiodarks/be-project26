package com.github.fiodarks.project26.archive.application.port.in;

public record HierarchyViewportPaginationResult(
        int limit,
        int returned,
        boolean truncated
) {
}

