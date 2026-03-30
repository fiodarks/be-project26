package com.github.fiodarks.project26.users.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository.MaterialSpringDataRepository;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.users.application.port.out.UserMaterialStats;
import com.github.fiodarks.project26.users.application.port.out.UserMaterialsStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserMaterialsStatsJpaAdapter implements UserMaterialsStatsPort {

    private final MaterialSpringDataRepository materials;

    @Override
    @Transactional(readOnly = true)
    public Map<UserId, UserMaterialStats> findStatsByUserIds(Set<UserId> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            return Map.of();
        }

        var ids = userIds.stream()
                .filter(Objects::nonNull)
                .map(UserId::value)
                .collect(Collectors.toUnmodifiableSet());

        var projections = materials.findCreatedByMaterialsStats(ids);
        return projections.stream()
                .collect(Collectors.toUnmodifiableMap(
                        p -> new UserId(p.getCreatedBy()),
                        p -> new UserMaterialStats(p.getMaterialsCount(), p.getLastMaterialCreatedAt()),
                        (left, right) -> left
                ));
    }
}
