package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.mapper;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.MaterialJpaEntity;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.Material;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.PartialDate;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.fiodarks.project26.commons.Commons.toNullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialPersistenceMapper {
    public static MaterialJpaEntity toEntity(Material material) {
        Objects.requireNonNull(material, "material");

        var entity = new MaterialJpaEntity();
        entity.setId(material.id().value());
        updateEntity(entity, material);
        return entity;
    }

    public static void updateEntity(MaterialJpaEntity entity, Material material) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(material, "material");

        entity.setTitle(material.title());
        entity.setLocation(material.location());
        entity.setPlaceId(material.placeId());
        if (material.geoPoint() != null) {
            entity.setLat(material.geoPoint().lat());
            entity.setLon(material.geoPoint().lon());
        } else {
            entity.setLat(null);
            entity.setLon(null);
        }
        entity.setCreationDateRaw(material.creationDate().raw());
        entity.setCreationDateFrom(material.creationDate().lowerBoundInclusive());
        entity.setCreationDateTo(material.creationDate().upperBoundInclusive());
        entity.setDescription(material.description());
        entity.setHierarchyId(material.hierarchyId().value());
        entity.setCreatedBy(material.createdBy().value());
        entity.setFileUrl(toNullable(material.fileUrl(), URI::toString));
        entity.setThumbnailUrl(toNullable(material.thumbnailUrl(), URI::toString));

        // Keep JPA-managed collections mutable; Hibernate may clear() them during merge.
        var metadata = entity.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            entity.setMetadata(metadata);
        } else {
            metadata.clear();
        }
        metadata.putAll(material.metadata());

        var tags = entity.getTags();
        if (tags == null) {
            tags = new java.util.ArrayList<>();
            entity.setTags(tags);
        } else {
            tags.clear();
        }
        tags.addAll(material.tags());
        entity.setCreatedAt(material.createdAt());
        entity.setUpdatedAt(material.updatedAt());
    }

    public static Material toDomain(MaterialJpaEntity entity) {
        Objects.requireNonNull(entity, "entity");

        var geoPoint = (entity.getLat() != null && entity.getLon() != null)
                ? new GeoPoint(entity.getLat(), entity.getLon())
                : null;

        var fileUrl = toNullable(entity.getFileUrl(), URI::create);
        var thumbnailUrl = toNullable(entity.getThumbnailUrl(), URI::create);

        return new Material(
                new MaterialId(entity.getId()),
                entity.getTitle(),
                entity.getLocation(),
                entity.getPlaceId(),
                geoPoint,
                PartialDate.parse(entity.getCreationDateRaw()),
                entity.getDescription(),
                new HierarchyNodeId(entity.getHierarchyId()),
                new UserId(entity.getCreatedBy()),
                fileUrl,
                thumbnailUrl,
                entity.getMetadata() == null ? Map.of() : Map.copyOf(entity.getMetadata()),
                entity.getTags(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
