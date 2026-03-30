package com.github.fiodarks.project26.archive.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.MaterialDTO;
import com.github.fiodarks.project26.archive.domain.model.Material;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialWebMapper {
    public static MaterialDTO toDto(Material material, String authorName, String authorSurname) {
        Objects.requireNonNull(material, "material");
        var dto = new MaterialDTO(
                material.id().value(),
                material.title(),
                material.location(),
                material.creationDate().raw(),
                material.description(),
                material.hierarchyId().value(),
                material.metadata(),
                material.createdAt()
        );

        dto.setAuthorName(authorName);
        dto.setAuthorSurname(authorSurname);
        dto.setPlaceId(material.placeId());
        if (material.geoPoint() != null) {
            dto.setLat(material.geoPoint().lat());
            dto.setLon(material.geoPoint().lon());
        }
        dto.setFileUrl(material.fileUrl());
        dto.setThumbnailUrl(material.thumbnailUrl());
        dto.setTags(material.tags());
        dto.setUpdatedAt(material.updatedAt());
        dto.setMetadata(Map.copyOf(material.metadata()));
        return dto;
    }
}
