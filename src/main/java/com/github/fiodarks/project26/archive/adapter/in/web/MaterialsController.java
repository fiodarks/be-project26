package com.github.fiodarks.project26.archive.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.api.MaterialsApi;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialDTO;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialMapPhoto;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialMapResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialMapPoint;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialPreviewsRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialPreviewsResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.MaterialPreviewDTO;
import com.github.fiodarks.project26.adapter.in.web.dto.UpdateMaterialCommand;
import com.github.fiodarks.project26.archive.adapter.in.web.mapper.MaterialMapWebMapper;
import com.github.fiodarks.project26.archive.adapter.in.web.mapper.MaterialPreviewWebMapper;
import com.github.fiodarks.project26.archive.adapter.in.web.mapper.MaterialWebMapper;
import com.github.fiodarks.project26.archive.adapter.in.web.security.SpringSecurityActorResolver;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventCommand;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventUseCase;
import com.github.fiodarks.project26.audit.domain.model.AuditAction;
import com.github.fiodarks.project26.archive.application.port.in.CreateMaterialCommand;
import com.github.fiodarks.project26.archive.application.port.in.MaterialUpload;
import com.github.fiodarks.project26.archive.application.port.in.SearchMaterialsRequest;
import com.github.fiodarks.project26.archive.application.port.in.UpdateMaterialRequest;
import com.github.fiodarks.project26.archive.application.service.ArchiveApplicationService;
import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.PartialDate;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.users.application.port.in.GetUserAccountQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.fiodarks.project26.commons.Commons.toNullable;

@RestController
@RequiredArgsConstructor
public class MaterialsController implements MaterialsApi {

    private final ArchiveApplicationService archive;
    private final SpringSecurityActorResolver actorResolver;
    private final GetUserAccountQuery userAccounts;
    private final RecordAuditEventUseCase auditRecorder;

    @Override
    public ResponseEntity<MaterialMapResponse> materialsGet(
            List<Double> bbox,
            String dateFrom,
            String dateTo,
            String search,
            List<String> filter,
            List<String> tags,
            UUID hierarchyLevelId,
            UUID userId
    ) {
        var from = toNullable(dateFrom, d -> PartialDate.parse(d).lowerBoundInclusive());
        var to = toNullable(dateTo, d -> PartialDate.parse(d).upperBoundInclusive());
        if (bbox == null || bbox.isEmpty()) {
            throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("bbox is required");
        }
        var geoBbox = GeoBoundingBox.fromBboxDoubles(bbox);

        Map<String, String> parsedFilter = Map.of();
        if (filter != null && !filter.isEmpty()) {
            var map = new LinkedHashMap<String, String>();
            for (var entry : filter) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                var idx = entry.indexOf('=');
                if (idx <= 0) {
                    throw new com.github.fiodarks.project26.archive.application.exception.ValidationException(
                            "Invalid filter entry: '" + entry + "'. Expected filter=key=value"
                    );
                }
                var key = entry.substring(0, idx).trim();
                var value = entry.substring(idx + 1).trim();
                if (key.isBlank()) {
                    throw new com.github.fiodarks.project26.archive.application.exception.ValidationException(
                            "Invalid filter entry: '" + entry + "'. Key must be non-blank"
                    );
                }
                if (map.putIfAbsent(key, value) != null) {
                    throw new com.github.fiodarks.project26.archive.application.exception.ValidationException(
                            "Duplicate filter key: " + key
                    );
                }
            }
            parsedFilter = Map.copyOf(map);
        }

        var request = SearchMaterialsRequest.builder()
                .searchPhrase(search)
                .hierarchyLevelId(toNullable(hierarchyLevelId, HierarchyNodeId::new))
                .createdBy(toNullable(userId, UserId::new))
                .dateFrom(from)
                .dateTo(to)
                .bbox(geoBbox)
                .filter(parsedFilter)
                .tags(tags)
                .build();

        var items = archive.search(request);
        var authorsById = resolveAuthorsById(items);

        var grouped = new LinkedHashMap<MaterialMapWebMapper.CoordKey, List<com.github.fiodarks.project26.archive.domain.model.Material>>();
        for (var material : items) {
            if (material.geoPoint() == null) {
                continue;
            }
            var key = MaterialMapWebMapper.CoordKey.from(material.geoPoint());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(material);
        }

        var points = grouped.entrySet().stream()
                .map(e -> MaterialMapWebMapper.toPointDto(e.getKey(), e.getValue()))
                .toList();
        enrichMapPointsWithAuthorNames(points, authorsById);

        var response = new MaterialMapResponse();
        response.setPoints(points);
        response.setTotalPhotos(grouped.values().stream().mapToInt(List::size).sum());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MaterialPreviewsResponse> materialsPreviewsPost(MaterialPreviewsRequest materialPreviewsRequest) {
        var actor = actorResolver.resolveActorOrAnonymous();
        Objects.requireNonNull(materialPreviewsRequest, "materialPreviewsRequest");

        var ids = materialPreviewsRequest.getIds();
        if (ids == null || ids.isEmpty()) {
            throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("ids is required");
        }
        if (ids.size() > 200) {
            throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("Too many ids; max is 200");
        }

        var materialIds = ids.stream().map(MaterialId::new).toList();
        var materials = archive.getByIds(actor, materialIds);
        var authorsById = resolveAuthorsById(materials);

        var found = materials.stream().map(m -> m.id().value()).collect(Collectors.toSet());
        var notFoundIds = ids.stream().filter(id -> !found.contains(id)).toList();

        var previews = materials.stream().map(MaterialPreviewWebMapper::toDto).toList();
        enrichMaterialPreviewsWithAuthorNames(previews, authorsById);

        var response = new MaterialPreviewsResponse();
        response.setData(previews);
        response.setNotFoundIds(notFoundIds);
        return ResponseEntity.ok(response);
    }

    @Override
    @Deprecated
    public ResponseEntity<MaterialPreviewsResponse> materialsPreviewsPost_0(MaterialPreviewsRequest materialPreviewsRequest) {
        return materialsPreviewsPost(materialPreviewsRequest);
    }

    @Override
    public ResponseEntity<MaterialDTO> materialsIdGet(UUID id) {
        var actor = actorResolver.resolveActorOrAnonymous();
        var material = archive.getById(actor, new MaterialId(id));
        var author = userAccounts.findById(material.createdBy()).orElse(null);
        return ResponseEntity.ok(MaterialWebMapper.toDto(
                material,
                author == null ? null : author.name(),
                author == null ? null : author.surname()
        ));
    }

    @Override
    public ResponseEntity<Void> materialsIdDelete(UUID id) {
        var actor = actorResolver.requireActor();
        var materialId = new MaterialId(id);
        archive.delete(actor, materialId);
        auditRecorder.record(new RecordAuditEventCommand(
                actor,
                AuditAction.MATERIAL_DELETED,
                null,
                materialId,
                null,
                null
        ));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MaterialDTO> materialsIdPut(UUID id, UpdateMaterialCommand updateMaterialCommand) {
        var actor = actorResolver.requireActor();
        Objects.requireNonNull(updateMaterialCommand, "updateMaterialCommand");

        var request = UpdateMaterialRequest.builder()
                .actor(actor)
                .id(new MaterialId(id))
                .title(updateMaterialCommand.getTitle())
                .location(updateMaterialCommand.getLocation())
                .creationDate(PartialDate.parse(updateMaterialCommand.getCreationDate()))
                .description(updateMaterialCommand.getDescription())
                .hierarchyId(new HierarchyNodeId(updateMaterialCommand.getHierarchyId()))
                .metadata(updateMaterialCommand.getMetadata() == null ? Map.of() : updateMaterialCommand.getMetadata())
                .tags(updateMaterialCommand.getTags())
                .build();

        var updated = archive.update(request);
        auditRecorder.record(new RecordAuditEventCommand(
                actor,
                AuditAction.MATERIAL_UPDATED,
                null,
                new MaterialId(id),
                null,
                null
        ));
        var author = userAccounts.findById(updated.createdBy()).orElse(null);
        return ResponseEntity.ok(MaterialWebMapper.toDto(
                updated,
                author == null ? null : author.name(),
                author == null ? null : author.surname()
        ));
    }

    @Override
    @PostMapping(
            value = "/materials",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MaterialDTO> materialsPost(
            @RequestParam("title") String title,
            @RequestParam("location") String location,
            @RequestParam("creationDate") String creationDate,
            @RequestParam("description") String description,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "placeId", required = false) String placeId,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "hierarchyId", required = false) UUID hierarchyId,
            @RequestParam(value = "metadata", required = false) Map<String, String> metadata,
            @RequestParam(value = "tags", required = false) List<String> tags
    ) {
        var actor = actorResolver.requireActor();

        GeoPoint geoPoint = null;
        if (lat != null || lon != null) {
            if (lat == null || lon == null) {
                throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("Both lat and lon are required when specifying geo point");
            }
            geoPoint = new GeoPoint(lat, lon);
        }

        if (file == null || file.isEmpty()) {
            throw new com.github.fiodarks.project26.archive.application.exception.ValidationException("file is required");
        }

        try (var inputStream = file.getInputStream()) {
            var upload = new MaterialUpload(
                    file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize(),
                    inputStream
            );

            var command = CreateMaterialCommand.builder()
                    .actor(actor)
                    .title(title)
                    .location(location)
                    .placeId(placeId)
                    .geoPoint(geoPoint)
                    .creationDate(PartialDate.parse(creationDate))
                    .description(description)
                    .hierarchyId(toNullable(hierarchyId, HierarchyNodeId::new))
                    .metadata(metadata == null ? Map.of() : metadata)
                    .tags(tags)
                    .upload(upload)
                    .build();

            var created = archive.create(command);
            var author = userAccounts.findById(created.createdBy()).orElse(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(MaterialWebMapper.toDto(
                    created,
                    author == null ? null : author.name(),
                    author == null ? null : author.surname()
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    private record AuthorNames(String name, String surname) {
    }

    private Map<UUID, AuthorNames> resolveAuthorsById(List<com.github.fiodarks.project26.archive.domain.model.Material> materials) {
        Objects.requireNonNull(materials, "materials");

        var userIds = materials.stream()
                .map(com.github.fiodarks.project26.archive.domain.model.Material::createdBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        var resolved = new HashMap<UUID, AuthorNames>(Math.max(16, userIds.size() * 2));
        for (var userId : userIds) {
            userAccounts.findById(userId).ifPresent(account -> resolved.put(
                    userId.value(),
                    new AuthorNames(account.name(), account.surname())
            ));
        }
        return Map.copyOf(resolved);
    }

    private static void enrichMapPointsWithAuthorNames(List<MaterialMapPoint> points, Map<UUID, AuthorNames> authorsById) {
        Objects.requireNonNull(points, "points");
        Objects.requireNonNull(authorsById, "authorsById");
        if (authorsById.isEmpty()) {
            return;
        }
        for (var point : points) {
            if (point == null || point.getPhotos() == null || point.getPhotos().isEmpty()) {
                continue;
            }
            for (var photo : point.getPhotos()) {
                enrichMaterialMapPhoto(photo, authorsById);
            }
        }
    }

    private static void enrichMaterialPreviewsWithAuthorNames(List<MaterialPreviewDTO> previews, Map<UUID, AuthorNames> authorsById) {
        Objects.requireNonNull(previews, "previews");
        Objects.requireNonNull(authorsById, "authorsById");
        if (authorsById.isEmpty()) {
            return;
        }
        for (var preview : previews) {
            if (preview == null) {
                continue;
            }
            var ownerId = preview.getOwnerId();
            if (ownerId == null) {
                continue;
            }
            var author = authorsById.get(ownerId);
            if (author == null) {
                continue;
            }
            preview.setAuthorName(author.name());
            preview.setAuthorSurname(author.surname());
        }
    }

    private static void enrichMaterialMapPhoto(MaterialMapPhoto photo, Map<UUID, AuthorNames> authorsById) {
        Objects.requireNonNull(authorsById, "authorsById");
        if (photo == null) {
            return;
        }
        var ownerId = photo.getOwnerId();
        if (ownerId == null) {
            return;
        }
        var author = authorsById.get(ownerId);
        if (author == null) {
            return;
        }
        photo.setAuthorName(author.name());
        photo.setAuthorSurname(author.surname());
    }
}
