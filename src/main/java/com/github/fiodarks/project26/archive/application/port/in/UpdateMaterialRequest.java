package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.PartialDate;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
public record UpdateMaterialRequest(
        Actor actor,
        MaterialId id,
        String title,
        String location,
        PartialDate creationDate,
        String description,
        HierarchyNodeId hierarchyId,
        Map<String, String> metadata,
        List<String> tags
) {
    public UpdateMaterialRequest {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(creationDate, "creationDate");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(hierarchyId, "hierarchyId");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
