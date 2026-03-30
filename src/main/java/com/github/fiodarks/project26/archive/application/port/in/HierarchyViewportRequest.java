package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import lombok.Builder;

import java.util.EnumSet;
import java.util.Objects;

@Builder
public record HierarchyViewportRequest(
        GeoBoundingBox bbox,
        String level,
        HierarchyNodeId parentId,
        String search,
        Integer limit,
        EnumSet<HierarchyViewportInclude> include
) {
    public HierarchyViewportRequest {
        Objects.requireNonNull(bbox, "bbox");
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("level must be non-blank");
        }
        include = include == null ? EnumSet.noneOf(HierarchyViewportInclude.class) : EnumSet.copyOf(include);
    }
}

