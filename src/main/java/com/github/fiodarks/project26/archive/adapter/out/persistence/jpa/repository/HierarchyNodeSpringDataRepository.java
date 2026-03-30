package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.HierarchyNodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HierarchyNodeSpringDataRepository extends JpaRepository<HierarchyNodeJpaEntity, UUID> {
    Optional<HierarchyNodeJpaEntity> findFirstByParentIdIsNullAndLevelAndNameIgnoreCase(int level, String name);

    Optional<HierarchyNodeJpaEntity> findFirstByParentIdIsNullAndLevel(int level);

    Optional<HierarchyNodeJpaEntity> findFirstByParentIdAndLevelAndNameIgnoreCase(UUID parentId, int level, String name);

    @Query("select distinct h.parentId from HierarchyNodeJpaEntity h where h.parentId in :parentIds")
    List<UUID> findParentIdsWithChildren(@Param("parentIds") Collection<UUID> parentIds);
}
