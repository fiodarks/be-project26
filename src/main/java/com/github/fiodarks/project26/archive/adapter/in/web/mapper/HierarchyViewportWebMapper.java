package com.github.fiodarks.project26.archive.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportNode;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportPagination;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportPathItem;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyViewportStats;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportNodeResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportPathItemResult;
import com.github.fiodarks.project26.archive.application.port.in.HierarchyViewportResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HierarchyViewportWebMapper {

    public static HierarchyViewportResponse toDto(HierarchyViewportResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        var response = new HierarchyViewportResponse();
        response.setLevel(result.level());
        response.setBbox(result.bbox());
        response.setPagination(toPagination(result.pagination().limit(), result.pagination().returned(), result.pagination().truncated()));
        response.setRoot(toRootNode(result.data()));
        return response;
    }

    private static HierarchyViewportPagination toPagination(int limit, int returned, boolean truncated) {
        var pagination = new HierarchyViewportPagination();
        pagination.setLimit(limit);
        pagination.setReturned(returned);
        pagination.setTruncated(truncated);
        return pagination;
    }

    private static HierarchyViewportNode toRootNode(List<HierarchyViewportNodeResult> flatNodes) {
        if (flatNodes == null || flatNodes.isEmpty()) {
            throw new IllegalArgumentException("flatNodes must not be null/empty");
        }

        var dtoById = new HashMap<java.util.UUID, HierarchyViewportNode>();
        for (var node : flatNodes) {
            var dto = toNode(node);
            dto.setChildren(new ArrayList<>());
            dtoById.put(dto.getId(), dto);
        }

        HierarchyViewportNode root = null;
        for (var dto : dtoById.values()) {
            var parentId = dto.getParentId();
            if (parentId == null) {
                if ("root".equals(dto.getLevel())) {
                    root = dto;
                }
                continue;
            }
            var parent = dtoById.get(parentId);
            if (parent != null) {
                parent.getChildren().add(dto);
            }
        }

        if (root == null) {
            // Fallback: choose a parentless node, if any.
            for (var dto : dtoById.values()) {
                if (dto.getParentId() == null) {
                    root = dto;
                    break;
                }
            }
        }
        if (root == null) {
            throw new IllegalStateException("Failed to build hierarchy tree: root node not present");
        }

        sortRecursively(root);
        return root;
    }

    private static void sortRecursively(HierarchyViewportNode node) {
        if (node == null) {
            return;
        }
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            node.setChildren(node.getChildren() == null ? List.of() : List.copyOf(node.getChildren()));
            return;
        }

        var collator = Collator.getInstance(Locale.ROOT);
        node.getChildren().sort(java.util.Comparator
                .comparing(HierarchyViewportNode::getName, collator)
                .thenComparing(n -> n.getId() == null ? "" : n.getId().toString())
        );
        for (var child : node.getChildren()) {
            sortRecursively(child);
        }
        node.setChildren(List.copyOf(node.getChildren()));
    }

    private static HierarchyViewportNode toNode(HierarchyViewportNodeResult node) {
        var dto = new HierarchyViewportNode();
        dto.setId(node.id().value());
        dto.setName(node.name());
        dto.setLevel(node.level());
        dto.setParentId(node.parentId() == null ? null : node.parentId().value());
        dto.setHasChildren(node.hasChildren());

        if (node.path() != null) {
            dto.setPath(node.path().stream().map(HierarchyViewportWebMapper::toPathItem).toList());
        }
        if (node.stats() != null) {
            var stats = new HierarchyViewportStats();
            stats.setPoints(node.stats().points());
            dto.setStats(stats);
        }
        if (node.extent() != null) {
            dto.setExtent(List.copyOf(node.extent()));
        }

        return dto;
    }

    private static HierarchyViewportPathItem toPathItem(HierarchyViewportPathItemResult item) {
        var dto = new HierarchyViewportPathItem();
        dto.setId(item.id().value());
        dto.setName(item.name());
        dto.setLevel(item.level());
        return dto;
    }
}
