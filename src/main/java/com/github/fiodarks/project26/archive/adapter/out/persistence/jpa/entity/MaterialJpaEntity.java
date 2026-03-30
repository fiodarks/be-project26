package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class MaterialJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "creation_date_raw", nullable = false)
    private String creationDateRaw;

    @Column(name = "creation_date_from", nullable = false)
    private LocalDate creationDateFrom;

    @Column(name = "creation_date_to", nullable = false)
    private LocalDate creationDateTo;

    @Column(name = "description", nullable = false, length = 10_000)
    private String description;

    @Column(name = "hierarchy_id", nullable = false)
    private UUID hierarchyId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "material_metadata", joinColumns = @JoinColumn(name = "material_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "material_tags", joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
