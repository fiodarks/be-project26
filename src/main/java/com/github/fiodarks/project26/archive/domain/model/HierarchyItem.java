package com.github.fiodarks.project26.archive.domain.model;

import java.util.Objects;

public record HierarchyItem(
        HierarchyNodeId id,
        HierarchyNodeId parentId,
        int level,
        String name,
        String description,
        HierarchyNodeStatus status
) {
    public HierarchyItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        if (level < 0) {
            throw new IllegalArgumentException("level must be >= 0");
        }
    }
}
