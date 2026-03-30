package com.github.fiodarks.project26.archive.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record HierarchyTree(HierarchyItem node, List<HierarchyTree> children) {
    public HierarchyTree {
        Objects.requireNonNull(node, "node");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public Optional<HierarchyTree> findSubtree(HierarchyNodeId id) {
        Objects.requireNonNull(id, "id");
        if (node.id().equals(id)) {
            return Optional.of(this);
        }
        for (var child : children) {
            var found = child.findSubtree(id);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public List<HierarchyNodeId> collectNodeIds() {
        var ids = new java.util.ArrayList<HierarchyNodeId>();
        collectNodeIdsInto(ids);
        return List.copyOf(ids);
    }

    private void collectNodeIdsInto(java.util.List<HierarchyNodeId> ids) {
        ids.add(node.id());
        for (var child : children) {
            child.collectNodeIdsInto(ids);
        }
    }
}
