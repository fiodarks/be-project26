package com.github.fiodarks.project26.archive.application.service;

import com.github.fiodarks.project26.archive.application.exception.ForbiddenOperationException;
import com.github.fiodarks.project26.archive.application.exception.NotFoundException;
import com.github.fiodarks.project26.archive.application.exception.ValidationException;
import com.github.fiodarks.project26.archive.application.port.in.CreateMaterialCommand;
import com.github.fiodarks.project26.archive.application.port.in.CreateMaterialUseCase;
import com.github.fiodarks.project26.archive.application.port.in.DeleteMaterialUseCase;
import com.github.fiodarks.project26.archive.application.port.in.GetHierarchyQuery;
import com.github.fiodarks.project26.archive.application.port.in.GetHierarchyViewportQuery;
import com.github.fiodarks.project26.archive.application.port.in.GetMaterialQuery;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportInclude;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportNodeResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportPaginationResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportPathItemResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportRequest;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportStatsResult;
import com.github.fiodarks.project26.archive.application.port.in.SearchMaterialsQuery;
import com.github.fiodarks.project26.archive.application.port.in.SearchMaterialsRequest;
import com.github.fiodarks.project26.archive.application.port.in.UpdateMaterialRequest;
import com.github.fiodarks.project26.archive.application.port.in.UpdateMaterialUseCase;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeQueryPort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeStorePort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyRepositoryPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialHierarchyAggregate;
import com.github.fiodarks.project26.archive.application.port.out.MaterialHierarchyAggregationPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialSearchCriteria;
import com.github.fiodarks.project26.archive.application.port.out.MaterialFileStoragePort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialRepositoryPort;
import com.github.fiodarks.project26.archive.application.port.out.ReverseGeocodingPort;
import com.github.fiodarks.project26.archive.application.port.out.UserBlockStatusPort;
import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import com.github.fiodarks.project26.archive.domain.model.HierarchyTree;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeStatus;
import com.github.fiodarks.project26.archive.domain.model.Material;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.HierarchyItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

@RequiredArgsConstructor
public class ArchiveApplicationService implements
        CreateMaterialUseCase,
        UpdateMaterialUseCase,
        DeleteMaterialUseCase,
        GetMaterialQuery,
        SearchMaterialsQuery,
        GetHierarchyQuery,
        GetHierarchyViewportQuery {

    private final MaterialRepositoryPort materialRepository;
    private final HierarchyRepositoryPort hierarchyRepository;
    private final HierarchyNodeStorePort hierarchyNodeStore;
    private final HierarchyNodeQueryPort hierarchyNodeQuery;
    private final MaterialHierarchyAggregationPort materialHierarchyAggregation;
    private final MaterialFileStoragePort fileStorage;
    private final ReverseGeocodingPort reverseGeocodingPort;
    private final Clock clock;
    private final UserBlockStatusPort userBlockStatusPort;

    @Override
    public Material create(CreateMaterialCommand command) {
        Objects.requireNonNull(command, "command");
        requireCreator(command.actor());
        requireNotBlockedFromUploading(command.actor());

        var hierarchyId = command.hierarchyId();
        if (hierarchyId == null) {
            if (command.geoPoint() == null) {
                throw new ValidationException("hierarchyId is required when geoPoint is not provided");
            }
            hierarchyId = resolveAndEnsureHierarchyFor(command.geoPoint());
        } else if (!hierarchyRepository.existsById(hierarchyId)) {
            throw new ValidationException("Hierarchy node does not exist: " + hierarchyId.value());
        }

        var id = new MaterialId(UUID.randomUUID());
        var stored = fileStorage.store(command.actor().userId(), id, command.upload());

        var now = OffsetDateTime.now(clock);
        var material = Material.builder()
                .id(id)
                .title(command.title())
                .location(command.location())
                .placeId(command.placeId())
                .geoPoint(command.geoPoint())
                .creationDate(command.creationDate())
                .description(command.description())
                .hierarchyId(hierarchyId)
                .createdBy(command.actor().userId())
                .fileUrl(stored.fileUrl())
                .thumbnailUrl(stored.thumbnailUrl())
                .metadata(command.metadata())
                .tags(command.tags())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return materialRepository.save(material);
    }

    @Override
    public Material update(UpdateMaterialRequest request) {
        Objects.requireNonNull(request, "request");
        requireCreator(request.actor());

        var existing = materialRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException("Material not found: " + request.id().value()));

        requireOwnerOrAdmin(request.actor(), existing);

        if (!hierarchyRepository.existsById(request.hierarchyId())) {
            throw new ValidationException("Hierarchy node does not exist: " + request.hierarchyId().value());
        }

        var now = OffsetDateTime.now(clock);
        var updated = existing.toBuilder()
                .title(request.title())
                .location(request.location())
                .creationDate(request.creationDate())
                .description(request.description())
                .hierarchyId(request.hierarchyId())
                .metadata(request.metadata())
                .tags(request.tags())
                .updatedAt(now)
                .build();

        return materialRepository.save(updated);
    }

    @Override
    public void delete(Actor actor, MaterialId id) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(id, "id");
        requireCreator(actor);

        var existing = materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Material not found: " + id.value()));

        requireOwnerOrAdmin(actor, existing);

        materialRepository.deleteById(id);
        fileStorage.delete(id);
    }

    @Override
    public Material getById(Actor actor, MaterialId id) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(id, "id");
        return materialRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Material not found: " + id.value()));
    }

    public List<Material> getByIds(Actor actor, List<MaterialId> ids) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ids, "ids");
        if (ids.isEmpty()) {
            return List.of();
        }

        var fetched = materialRepository.findByIds(ids);
        if (fetched.isEmpty()) {
            return List.of();
        }

        var byId = new HashMap<MaterialId, Material>(fetched.size());
        for (var material : fetched) {
            byId.put(material.id(), material);
        }

        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Material> search(SearchMaterialsRequest request) {
        Objects.requireNonNull(request, "request");

        List<HierarchyNodeId> hierarchyIds = List.of();
        if (request.hierarchyLevelId() != null) {
            var subtree = hierarchyRepository.getTree().findSubtree(request.hierarchyLevelId())
                    .orElseThrow(() -> new ValidationException("Hierarchy node does not exist: " + request.hierarchyLevelId().value()));
            hierarchyIds = subtree.collectNodeIds();
        }

        var criteria = MaterialSearchCriteria.builder()
                .location(request.location())
                .placeId(request.placeId())
                .searchPhrase(request.searchPhrase())
                .hierarchyIds(hierarchyIds)
                .dateFrom(request.dateFrom())
                .dateTo(request.dateTo())
                .bbox(request.bbox())
                .filter(request.filter())
                .tags(request.tags())
                .build();

        return materialRepository.search(criteria);
    }

    @Override
    public HierarchyTree getHierarchy() {
        return hierarchyRepository.getTree();
    }

    @Override
    public HierarchyViewportResult getHierarchyViewport(HierarchyViewportRequest request) {
        Objects.requireNonNull(request, "request");

        var bbox = request.bbox();
        if (bbox.minLon() >= bbox.maxLon()) {
            throw new ValidationException("bbox minLon must be < maxLon");
        }
        if (bbox.minLat() >= bbox.maxLat()) {
            throw new ValidationException("bbox minLat must be < maxLat");
        }

        var targetLevel = parseLevel(request.level());
        var levelSlug = levelSlug(targetLevel);

        var rawLimit = request.limit() == null ? 500 : request.limit();
        if (rawLimit <= 0) {
            throw new ValidationException("limit must be > 0");
        }
        if (rawLimit > 2000) {
            throw new ValidationException("limit must be <= 2000");
        }
        var limit = rawLimit;

        var include = request.include();
        var aggregates = materialHierarchyAggregation.aggregateByHierarchyIdInBbox(bbox);
        if (aggregates.isEmpty()) {
            var root = hierarchyNodeStore.findRoot()
                    .orElseGet(() -> hierarchyNodeStore.save(new HierarchyItem(
                            new HierarchyNodeId(UUID.randomUUID()),
                            null,
                            0,
                            "Archive",
                            "Auto-created hierarchy root",
                            HierarchyNodeStatus.APPROVED
                    )));

            var parentIdsWithChildren = hierarchyNodeQuery.findParentIdsWithChildren(List.of(root.id()));
            var rootResult = new HierarchyViewportNodeResult(
                    root.id(),
                    root.name(),
                    levelSlug(root.level()),
                    root.parentId(),
                    parentIdsWithChildren.contains(root.id()),
                    include.contains(HierarchyViewportInclude.ANCESTORS) ? List.of() : null,
                    null,
                    null
            );

            return new HierarchyViewportResult(
                    levelSlug,
                    List.of(bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat()),
                    List.of(rootResult),
                    new HierarchyViewportPaginationResult(limit, 0, false)
            );
        }

        var leafIds = aggregates.stream()
                .map(MaterialHierarchyAggregate::hierarchyId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        var nodeById = loadAncestorsUntilLevel(leafIds, targetLevel);

        var byTarget = new HashMap<HierarchyNodeId, ViewportAcc>();
        for (var agg : aggregates) {
            var leaf = nodeById.get(agg.hierarchyId());
            if (leaf == null) {
                throw new ValidationException("Hierarchy node does not exist: " + agg.hierarchyId().value());
            }

            var target = resolveAncestorAtLevel(leaf, targetLevel, nodeById);
            if (target == null) {
                continue;
            }

            var extent = agg.extent();
            byTarget.merge(
                    target.id(),
                    new ViewportAcc(target, agg.points(), extent.minLon(), extent.minLat(), extent.maxLon(), extent.maxLat()),
                    (a, b) -> new ViewportAcc(
                            a.node(),
                            a.points() + b.points(),
                            Math.min(a.minLon(), b.minLon()),
                            Math.min(a.minLat(), b.minLat()),
                            Math.max(a.maxLon(), b.maxLon()),
                            Math.max(a.maxLat(), b.maxLat())
                    )
            );
        }

        var targetNodes = byTarget.values().stream().map(ViewportAcc::node).toList();

        // We always return a tree rooted at the archive root, so we must have the full parent chain available
        // regardless of whether the client asks for breadcrumb `path` expansion.
        loadAncestorsToRoot(
                targetNodes.stream().map(HierarchyItem::id).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                nodeById
        );

        if (request.parentId() != null) {
            var parentId = request.parentId();
            targetNodes = targetNodes.stream()
                    .filter(n -> isWithinSubtree(n, parentId, nodeById))
                    .toList();
        }

        if (request.search() != null && !request.search().isBlank()) {
            var needle = request.search().toLowerCase(Locale.ROOT);
            targetNodes = targetNodes.stream()
                    .filter(n -> n.name() != null && n.name().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        var collator = java.text.Collator.getInstance(Locale.ROOT);
        var sortedTargets = targetNodes.stream()
                .sorted(java.util.Comparator
                        .comparing(HierarchyItem::name, collator)
                        .thenComparing(i -> i.id().value().toString()))
                .toList();

        var truncated = sortedTargets.size() > limit;
        var pagedTargets = truncated ? sortedTargets.subList(0, limit) : sortedTargets;

        var included = new HashMap<HierarchyNodeId, ViewportAcc>();
        for (var target : pagedTargets) {
            var acc = byTarget.get(target.id());
            if (acc == null) {
                continue;
            }

            var current = target;
            while (current != null && current.level() <= targetLevel) {
                included.merge(
                        current.id(),
                        new ViewportAcc(current, acc.points(), acc.minLon(), acc.minLat(), acc.maxLon(), acc.maxLat()),
                        (a, b) -> new ViewportAcc(
                                a.node(),
                                a.points() + b.points(),
                                Math.min(a.minLon(), b.minLon()),
                                Math.min(a.minLat(), b.minLat()),
                                Math.max(a.maxLon(), b.maxLon()),
                                Math.max(a.maxLat(), b.maxLat())
                        )
                );

                var pid = current.parentId();
                if (pid == null) {
                    break;
                }
                current = nodeById.get(pid);
            }
        }

        var includedNodes = included.values().stream().map(ViewportAcc::node).toList();

        var sortedIncluded = includedNodes.stream()
                .sorted(java.util.Comparator
                        .comparingInt(HierarchyItem::level)
                        .thenComparing(HierarchyItem::name, collator)
                        .thenComparing(i -> i.id().value().toString()))
                .toList();

        var parentIdsWithChildren = hierarchyNodeQuery.findParentIdsWithChildren(sortedIncluded.stream().map(HierarchyItem::id).toList());

        var data = sortedIncluded.stream()
                .map(node -> toViewportNodeResult(node, included.get(node.id()), parentIdsWithChildren, include, nodeById))
                .filter(Objects::nonNull)
                .toList();

        return new HierarchyViewportResult(
                levelSlug,
                List.of(bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat()),
                data,
                new HierarchyViewportPaginationResult(limit, pagedTargets.size(), truncated)
        );
    }

    private java.util.Optional<HierarchyItem> ensureChild(HierarchyItem parent, String type, String name) {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (name == null || name.isBlank()) {
            return java.util.Optional.empty();
        }

        var level = parent.level() + 1;
        var existing = hierarchyNodeStore.findChildByParentLevelAndName(parent.id(), level, name);
        if (existing.isPresent()) {
            return existing;
        }

        var created = new HierarchyItem(
                new HierarchyNodeId(UUID.randomUUID()),
                parent.id(),
                level,
                name,
                "Auto-created " + type + " from map pin",
                HierarchyNodeStatus.PENDING
        );
        return java.util.Optional.of(hierarchyNodeStore.save(created));
    }

    private HierarchyNodeId resolveAndEnsureHierarchyFor(GeoPoint point) {
        var geocode = reverseGeocodingPort.reverse(point)
                .orElseThrow(() -> new ValidationException("Failed to reverse geocode point"));

        var root = hierarchyNodeStore.findRoot()
                .orElseGet(() -> hierarchyNodeStore.save(new HierarchyItem(
                        new HierarchyNodeId(UUID.randomUUID()),
                        null,
                        0,
                        "Archive",
                        "Auto-created hierarchy root",
                        HierarchyNodeStatus.APPROVED
                )));

        var current = root;
        current = ensureChild(current, "Country", geocode.country()).orElse(current);
        current = ensureChild(current, "Region", geocode.region()).orElse(current);

        if (geocode.city() == null || geocode.city().isBlank()) {
            throw new ValidationException("Geocoding did not return a city for provided point");
        }
        current = ensureChild(current, "City", geocode.city()).orElseThrow();

        current = ensureChild(current, "District", geocode.district()).orElse(current);
        return current.id();
    }

    private static void requireCreator(Actor actor) {
        if (actor.has(Role.ADMIN)) {
            return;
        }
        if (!actor.has(Role.CREATOR)) {
            throw new ForbiddenOperationException("Creator role required");
        }
    }

    private void requireNotBlockedFromUploading(Actor actor) {
        Objects.requireNonNull(actor, "actor");
        if (actor.has(Role.ADMIN)) {
            return;
        }
        var now = OffsetDateTime.now(clock);
        if (userBlockStatusPort != null && userBlockStatusPort.isBlocked(actor.userId(), now)) {
            throw new ForbiddenOperationException("User is blocked from adding materials");
        }
    }

    private static void requireOwnerOrAdmin(Actor actor, Material material) {
        if (actor.has(Role.ADMIN)) {
            return;
        }
        if (!material.createdBy().equals(actor.userId())) {
            throw new ForbiddenOperationException("Not a creator of material: " + material.id().value());
        }
    }

    private record ViewportAcc(HierarchyItem node, long points, double minLon, double minLat, double maxLon, double maxLat) {
    }

    private static HierarchyViewportNodeResult toViewportNodeResult(
            HierarchyItem node,
            ViewportAcc acc,
            Set<HierarchyNodeId> parentIdsWithChildren,
            java.util.EnumSet<HierarchyViewportInclude> include,
            java.util.Map<HierarchyNodeId, HierarchyItem> nodeById
    ) {
        if (acc == null) {
            return null;
        }

        var path = include.contains(HierarchyViewportInclude.ANCESTORS) ? buildPath(node, nodeById) : null;
        var stats = include.contains(HierarchyViewportInclude.COUNTS) ? new HierarchyViewportStatsResult(acc.points()) : null;
        var extent = include.contains(HierarchyViewportInclude.BBOX)
                ? List.of(acc.minLon(), acc.minLat(), acc.maxLon(), acc.maxLat())
                : null;

        return new HierarchyViewportNodeResult(
                node.id(),
                node.name(),
                levelSlug(node.level()),
                node.parentId(),
                parentIdsWithChildren.contains(node.id()),
                path,
                stats,
                extent
        );
    }

    private static int parseLevel(String raw) {
        var trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("level is required");
        }
        try {
            var numeric = Integer.parseInt(trimmed);
            if (numeric < 0 || numeric > 4) {
                throw new ValidationException("Unsupported hierarchy level: " + numeric);
            }
            return numeric;
        } catch (NumberFormatException ignored) {
            // continue
        }

        return switch (trimmed.toLowerCase(Locale.ROOT)) {
            case "root", "archive" -> 0;
            case "country" -> 1;
            case "region" -> 2;
            case "city" -> 3;
            case "district" -> 4;
            default -> throw new ValidationException("Unknown hierarchy level: " + trimmed);
        };
    }

    private static String levelSlug(int level) {
        return switch (level) {
            case 0 -> "root";
            case 1 -> "country";
            case 2 -> "region";
            case 3 -> "city";
            case 4 -> "district";
            default -> "level-" + level;
        };
    }

    private Map<HierarchyNodeId, HierarchyItem> loadAncestorsUntilLevel(Set<HierarchyNodeId> startingIds, int targetLevel) {
        Objects.requireNonNull(startingIds, "startingIds");
        if (startingIds.isEmpty()) {
            return new HashMap<>();
        }

        var nodeById = new HashMap<HierarchyNodeId, HierarchyItem>();
        var toLoad = new java.util.HashSet<>(startingIds);

        while (!toLoad.isEmpty()) {
            var batch = toLoad.stream().toList();
            toLoad.clear();

            var fetched = hierarchyNodeQuery.findByIds(batch);
            for (var item : fetched) {
                nodeById.put(item.id(), item);
            }

            for (var item : fetched) {
                if (item.level() > targetLevel && item.parentId() != null && !nodeById.containsKey(item.parentId())) {
                    toLoad.add(item.parentId());
                }
            }
        }

        return nodeById;
    }

    private void loadAncestorsToRoot(Set<HierarchyNodeId> startingIds, java.util.Map<HierarchyNodeId, HierarchyItem> nodeById) {
        Objects.requireNonNull(startingIds, "startingIds");
        Objects.requireNonNull(nodeById, "nodeById");

        var toLoad = new java.util.HashSet<HierarchyNodeId>();
        for (var id : startingIds) {
            var item = nodeById.get(id);
            if (item == null) {
                toLoad.add(id);
                continue;
            }
            if (item.parentId() != null && !nodeById.containsKey(item.parentId())) {
                toLoad.add(item.parentId());
            }
        }

        while (!toLoad.isEmpty()) {
            var batch = toLoad.stream().toList();
            toLoad.clear();

            var fetched = hierarchyNodeQuery.findByIds(batch);
            for (var item : fetched) {
                if (nodeById.putIfAbsent(item.id(), item) == null) {
                    if (item.parentId() != null && !nodeById.containsKey(item.parentId())) {
                        toLoad.add(item.parentId());
                    }
                }
            }
        }
    }

    private static HierarchyItem resolveAncestorAtLevel(
            HierarchyItem node,
            int targetLevel,
            java.util.Map<HierarchyNodeId, HierarchyItem> nodeById
    ) {
        var current = node;
        while (current != null && current.level() > targetLevel) {
            var parentId = current.parentId();
            if (parentId == null) {
                return null;
            }
            current = nodeById.get(parentId);
        }
        if (current == null || current.level() != targetLevel) {
            return null;
        }
        return current;
    }

    private static boolean isWithinSubtree(
            HierarchyItem node,
            HierarchyNodeId parentId,
            java.util.Map<HierarchyNodeId, HierarchyItem> nodeById
    ) {
        var current = node;
        while (current != null) {
            if (current.id().equals(parentId)) {
                return true;
            }
            var pid = current.parentId();
            if (pid == null) {
                return false;
            }
            current = nodeById.get(pid);
        }
        return false;
    }

    private static List<HierarchyViewportPathItemResult> buildPath(
            HierarchyItem node,
            java.util.Map<HierarchyNodeId, HierarchyItem> nodeById
    ) {
        var reversed = new java.util.ArrayList<HierarchyViewportPathItemResult>();
        var currentId = node.parentId();
        while (currentId != null) {
            var parent = nodeById.get(currentId);
            if (parent == null) {
                break;
            }
            reversed.add(new HierarchyViewportPathItemResult(parent.id(), parent.name(), levelSlug(parent.level())));
            currentId = parent.parentId();
        }
        java.util.Collections.reverse(reversed);
        return List.copyOf(reversed);
    }
}
