package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;

import java.util.List;

public interface MaterialHierarchyAggregationPort {
    List<MaterialHierarchyAggregate> aggregateByHierarchyIdInBbox(GeoBoundingBox bbox);
}

