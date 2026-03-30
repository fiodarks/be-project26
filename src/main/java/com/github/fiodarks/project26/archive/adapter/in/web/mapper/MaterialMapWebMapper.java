package com.github.fiodarks.project26.archive.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.MaterialMapPhoto;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialMapPoint;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import com.github.fiodarks.project26.archive.domain.model.Material;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialMapWebMapper {

    private static final double COORD_SCALE = 1_000_000d;

    public record CoordKey(int latE6, int lonE6) {
        public static CoordKey from(GeoPoint point) {
            Objects.requireNonNull(point, "point");
            return new CoordKey(
                    (int) Math.round(point.lat() * COORD_SCALE),
                    (int) Math.round(point.lon() * COORD_SCALE)
            );
        }

        public double lat() {
            return latE6 / COORD_SCALE;
        }

        public double lon() {
            return lonE6 / COORD_SCALE;
        }
    }

    public static MaterialMapPoint toPointDto(CoordKey key, List<Material> materialsAtPoint) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(materialsAtPoint, "materialsAtPoint");

        var point = new MaterialMapPoint();
        point.setLat(key.lat());
        point.setLon(key.lon());

        var title = materialsAtPoint.stream()
                .map(Material::location)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse(null);
        point.setTitle(title);

        var photos = materialsAtPoint.stream()
                .map(MaterialMapWebMapper::toPhotoDto)
                .sorted(Comparator
                        .comparingInt(MaterialMapPhoto::getYear).reversed()
                        .thenComparing(MaterialMapPhoto::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        point.setPhotos(photos);

        return point;
    }

    public static MaterialMapPhoto toPhotoDto(Material material) {
        Objects.requireNonNull(material, "material");

        var dto = new MaterialMapPhoto();
        dto.setId(material.id().value());
        dto.setTitle(material.title());
        dto.setYear(material.creationDate().lowerBoundInclusive().getYear());
        return dto;
    }
}

