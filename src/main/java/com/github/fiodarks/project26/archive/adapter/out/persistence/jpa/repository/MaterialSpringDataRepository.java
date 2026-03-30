package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.MaterialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

public interface MaterialSpringDataRepository extends JpaRepository<MaterialJpaEntity, UUID> {

    interface CreatedByMaterialsStatsProjection {
        UUID getCreatedBy();

        long getMaterialsCount();

        OffsetDateTime getLastMaterialCreatedAt();
    }

    @Query("""
            select
              m.createdBy as createdBy,
              count(m) as materialsCount,
              max(m.createdAt) as lastMaterialCreatedAt
            from MaterialJpaEntity m
            where m.createdBy in :userIds
            group by m.createdBy
            """)
    java.util.List<CreatedByMaterialsStatsProjection> findCreatedByMaterialsStats(@Param("userIds") Collection<UUID> userIds);
}
