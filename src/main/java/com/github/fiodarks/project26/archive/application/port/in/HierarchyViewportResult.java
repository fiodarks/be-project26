package com.github.fiodarks.project26.archive.application.port.in;

import java.util.List;

public record HierarchyViewportResult(
        String level,
        List<Double> bbox,
        List<HierarchyViewportNodeResult> data,
        HierarchyViewportPaginationResult pagination
) {
}
