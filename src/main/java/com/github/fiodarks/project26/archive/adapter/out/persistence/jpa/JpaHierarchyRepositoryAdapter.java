package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.mapper.HierarchyPersistenceMapper;
import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository.HierarchyNodeSpringDataRepository;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeStorePort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyNodeQueryPort;
import com.github.fiodarks.project26.archive.application.port.out.HierarchyRepositoryPort;
import com.github.fiodarks.project26.archive.domain.model.HierarchyItem;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeStatus;
import com.github.fiodarks.project26.archive.domain.model.HierarchyTree;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaHierarchyRepositoryAdapter implements HierarchyRepositoryPort, HierarchyNodeStorePort, HierarchyNodeQueryPort {

    private final HierarchyNodeSpringDataRepository repository;

    @Override
    public boolean existsById(HierarchyNodeId id) {
        Objects.requireNonNull(id, "id");
        return repository.existsById(id.value());
    }

    @Override
    public HierarchyTree getTree() {
        var nodes = repository.findAll().stream()
                .map(HierarchyPersistenceMapper::toDomain)
                .sorted(Comparator.comparingInt(HierarchyItem::level).thenComparing(HierarchyItem::name))
                .toList();

        if (nodes.isEmpty()) {
            var root = new HierarchyItem(
                    new HierarchyNodeId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                    null,
                    0,
                    "Archive",
                    "Empty hierarchy (bootstrap not executed)",
                    HierarchyNodeStatus.APPROVED
            );
            return new HierarchyTree(root, List.of());
        }

        var childrenByParent = new HashMap<HierarchyNodeId, List<HierarchyItem>>();
        var roots = new ArrayList<HierarchyItem>();
        for (var item : nodes) {
            if (item.parentId() == null) {
                roots.add(item);
            } else {
                childrenByParent.computeIfAbsent(item.parentId(), ignored -> new ArrayList<>()).add(item);
            }
        }

        roots.sort(Comparator.comparingInt(HierarchyItem::level).thenComparing(HierarchyItem::name));
        if (roots.size() == 1) {
            return toTree(roots.getFirst(), childrenByParent);
        }

        var syntheticRoot = new HierarchyItem(
                new HierarchyNodeId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                null,
                0,
                "Archive",
                "Synthetic root",
                HierarchyNodeStatus.APPROVED
        );
        var children = roots.stream().map(r -> toTree(r, childrenByParent)).toList();
        return new HierarchyTree(syntheticRoot, children);
    }

    @Override
    public Optional<HierarchyItem> findRoot() {
        var byName = repository.findFirstByParentIdIsNullAndLevelAndNameIgnoreCase(0, "Archive");
        if (byName.isPresent()) {
            return byName.map(HierarchyPersistenceMapper::toDomain);
        }
        return repository.findFirstByParentIdIsNullAndLevel(0).map(HierarchyPersistenceMapper::toDomain);
    }

    @Override
    public Optional<HierarchyItem> findChildByParentLevelAndName(HierarchyNodeId parentId, int level, String name) {
        Objects.requireNonNull(parentId, "parentId");
        Objects.requireNonNull(name, "name");
        return repository.findFirstByParentIdAndLevelAndNameIgnoreCase(parentId.value(), level, name)
                .map(HierarchyPersistenceMapper::toDomain);
    }

    @Override
    public HierarchyItem save(HierarchyItem item) {
        Objects.requireNonNull(item, "item");
        var entity = repository.findById(item.id().value()).orElseGet(com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.HierarchyNodeJpaEntity::new);
        entity.setId(item.id().value());
        entity.setParentId(item.parentId() == null ? null : item.parentId().value());
        entity.setLevel(item.level());
        entity.setName(item.name());
        entity.setDescription(item.description());
        entity.setStatus(item.status().name());
        var saved = repository.save(entity);
        return HierarchyPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<HierarchyItem> findByIds(List<HierarchyNodeId> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids.stream().map(HierarchyNodeId::value).toList())
                .stream()
                .map(HierarchyPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Set<HierarchyNodeId> findParentIdsWithChildren(List<HierarchyNodeId> parentIds) {
        Objects.requireNonNull(parentIds, "parentIds");
        if (parentIds.isEmpty()) {
            return Set.of();
        }
        return repository.findParentIdsWithChildren(parentIds.stream().map(HierarchyNodeId::value).toList())
                .stream()
                .filter(Objects::nonNull)
                .map(HierarchyNodeId::new)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static HierarchyTree toTree(HierarchyItem node, Map<HierarchyNodeId, List<HierarchyItem>> childrenByParent) {
        var children = childrenByParent.getOrDefault(node.id(), List.of());
        var childTrees = children.stream().map(c -> toTree(c, childrenByParent)).toList();
        return new HierarchyTree(node, childTrees);
    }
}
