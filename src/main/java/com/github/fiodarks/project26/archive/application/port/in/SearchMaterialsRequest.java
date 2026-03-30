package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
public record SearchMaterialsRequest(
        String location,
        String placeId,
        String searchPhrase,
        HierarchyNodeId hierarchyLevelId,
        LocalDate dateFrom,
        LocalDate dateTo,
        GeoBoundingBox bbox,
        Map<String, String> filter,
        List<String> tags
) {
    public SearchMaterialsRequest {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be <= dateTo");
        }

        filter = filter == null ? Map.of() : Map.copyOf(filter);
        tags = tags == null ? List.of() : List.copyOf(tags);

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

        Objects.requireNonNull(searchPhrase == null ? "" : searchPhrase);
    }
}
