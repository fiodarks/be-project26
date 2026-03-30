package com.github.fiodarks.project26.archive.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.HierarchyNode;
import com.github.fiodarks.project26.archive.domain.model.HierarchyTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.github.fiodarks.project26.commons.Commons.toNullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HierarchyWebMapper {
    public static HierarchyNode toDto(HierarchyTree tree) {
        Objects.requireNonNull(tree, "tree");

        var node = new HierarchyNode();
        node.setId(tree.node().id().value());
        node.parentId(toNullable(tree.node().parentId(), id -> id.value()));
        node.setLevel(tree.node().level());
        node.setName(tree.node().name());
        node.setDescription(tree.node().description());
        node.setChildren(tree.children().stream().map(HierarchyWebMapper::toDto).toList());
        return node;
    }
}
