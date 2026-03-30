package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.HierarchyTree;

public interface HierarchyRepositoryPort {
    boolean existsById(HierarchyNodeId id);

    HierarchyTree getTree();
}

