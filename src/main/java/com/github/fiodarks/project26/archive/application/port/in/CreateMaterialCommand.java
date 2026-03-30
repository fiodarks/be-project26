package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.PartialDate;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
public record CreateMaterialCommand(
        Actor actor,
        String title,
        String location,
        String placeId,
        GeoPoint geoPoint,
        PartialDate creationDate,
        String description,
        HierarchyNodeId hierarchyId,
        Map<String, String> metadata,
        List<String> tags,
        MaterialUpload upload
) {
    public CreateMaterialCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(creationDate, "creationDate");
        Objects.requireNonNull(description, "description");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        tags = tags == null ? List.of() : List.copyOf(tags);
        Objects.requireNonNull(upload, "upload");
    }
}
