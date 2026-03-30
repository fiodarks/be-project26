package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.HierarchyItem;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;

import java.util.List;
import java.util.Set;

public interface HierarchyNodeQueryPort {
    List<HierarchyItem> findByIds(List<HierarchyNodeId> ids);

    Set<HierarchyNodeId> findParentIdsWithChildren(List<HierarchyNodeId> parentIds);
}

