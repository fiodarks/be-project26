package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;

public record MaterialHierarchyAggregate(
        HierarchyNodeId hierarchyId,
        long points,
        GeoBoundingBox extent
) {
}

