package com.github.fiodarks.project26.archive.domain.model;

import lombok.Builder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder(toBuilder = true)
public record Material(
        MaterialId id,
        String title,
        String location,
        String placeId,
        GeoPoint geoPoint,
        PartialDate creationDate,
        String description,
        HierarchyNodeId hierarchyId,
        UserId createdBy,
        URI fileUrl,
        URI thumbnailUrl,
        Map<String, String> metadata,
        List<String> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Material {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(creationDate, "creationDate");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(hierarchyId, "hierarchyId");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        metadata = Map.copyOf(metadata);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
