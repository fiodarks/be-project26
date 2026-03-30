package com.github.fiodarks.project26.users.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.application.port.out.UserBlockStatusPort;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository.UserAccountSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserBlockStatusJpaAdapter implements UserBlockStatusPort {

    private final UserAccountSpringDataRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(UserId userId, OffsetDateTime at) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(at, "at");
        var entity = repository.findById(userId.value()).orElse(null);
        if (entity == null || entity.getBlockedUntil() == null) {
            return false;
        }
        return at.isBefore(entity.getBlockedUntil());
    }
}

