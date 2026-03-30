package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;

import java.util.List;

public record HierarchyViewportNodeResult(
        HierarchyNodeId id,
        String name,
        String level,
        HierarchyNodeId parentId,
        boolean hasChildren,
        List<HierarchyViewportPathItemResult> path,
        HierarchyViewportStatsResult stats,
        List<Double> extent
) {
}

