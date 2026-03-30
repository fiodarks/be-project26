package com.github.fiodarks.project26.archive.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.MaterialPreviewDTO;
import com.github.fiodarks.project26.archive.domain.model.Material;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialPreviewWebMapper {

    public static MaterialPreviewDTO toDto(Material material) {
        Objects.requireNonNull(material, "material");
        var dto = new MaterialPreviewDTO();
        dto.setId(material.id().value());
        dto.setTitle(material.title());
        dto.setYear(material.creationDate().lowerBoundInclusive().getYear());
        dto.setThumbnailUrl(material.thumbnailUrl());
        dto.setFileUrl(material.fileUrl());
        return dto;
    }
}

