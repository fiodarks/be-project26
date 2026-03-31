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
        Objects.requireNonNull(material.createdBy(), "material.createdBy");

        var dto = new MaterialDTO();
        dto.setId(material.id().value());
        dto.setOwnerId(material.createdBy().value());
        dto.setTitle(material.title());
        dto.setLocation(material.location());
        dto.setCreationDate(material.creationDate().raw());
        dto.setDescription(material.description());
        dto.setHierarchyId(material.hierarchyId().value());
        dto.setCreatedAt(material.createdAt());

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
