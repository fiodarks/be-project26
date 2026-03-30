package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record MaterialSearchCriteria(
        String location,
        String placeId,
        String searchPhrase,
        List<HierarchyNodeId> hierarchyIds,
        UserId createdBy,
        LocalDate dateFrom,
        LocalDate dateTo,
        GeoBoundingBox bbox,
        Map<String, String> filter,
        List<String> tags
) {
    public MaterialSearchCriteria {
        hierarchyIds = hierarchyIds == null ? List.of() : List.copyOf(hierarchyIds);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
        tags = tags == null ? List.of() : List.copyOf(tags);

        if (hierarchyIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("hierarchyIds must not contain nulls");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be <= dateTo");
        }
        for (var entry : filter.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("filter keys must be non-blank");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("filter values must be non-null");
            }
        }
        for (var tag : tags) {
            if (tag == null || tag.isBlank()) {
                throw new IllegalArgumentException("tags must be non-blank");
            }
        }
    }
}
