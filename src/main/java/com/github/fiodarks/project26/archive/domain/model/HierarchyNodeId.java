package com.github.fiodarks.project26.archive.domain.model;

import java.util.Objects;
import java.util.UUID;

public record HierarchyNodeId(UUID value) {
    public HierarchyNodeId {
        Objects.requireNonNull(value, "value");
    }
}

