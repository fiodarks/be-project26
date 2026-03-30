package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.HierarchyItem;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;

import java.util.Optional;

public interface HierarchyNodeStorePort {
    Optional<HierarchyItem> findRoot();

    Optional<HierarchyItem> findChildByParentLevelAndName(HierarchyNodeId parentId, int level, String name);

    HierarchyItem save(HierarchyItem item);
}

