package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.mapper;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.HierarchyNodeJpaEntity;
import com.github.fiodarks.project26.archive.domain.model.HierarchyItem;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.github.fiodarks.project26.commons.Commons.toNullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HierarchyPersistenceMapper {
    public static HierarchyItem toDomain(HierarchyNodeJpaEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return new HierarchyItem(
                new HierarchyNodeId(entity.getId()),
                toNullable(entity.getParentId(), HierarchyNodeId::new),
                entity.getLevel(),
                entity.getName(),
                entity.getDescription(),
                toStatus(entity.getStatus())
        );
    }

    private static HierarchyNodeStatus toStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return HierarchyNodeStatus.APPROVED;
        }
        return HierarchyNodeStatus.valueOf(raw);
    }
}
