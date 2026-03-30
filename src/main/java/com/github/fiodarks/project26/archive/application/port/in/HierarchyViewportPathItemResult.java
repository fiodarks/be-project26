package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;

public record HierarchyViewportPathItemResult(
        HierarchyNodeId id,
        String name,
        String level
) {
}

