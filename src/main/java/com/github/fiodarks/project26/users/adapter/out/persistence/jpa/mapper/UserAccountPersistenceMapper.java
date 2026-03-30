package com.github.fiodarks.project26.users.adapter.out.persistence.jpa.mapper;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.entity.UserAccountJpaEntity;
import com.github.fiodarks.project26.users.domain.model.UserAccount;

import java.util.Objects;

public final class UserAccountPersistenceMapper {
    private UserAccountPersistenceMapper() {
    }

    public static UserAccountJpaEntity toEntity(UserAccount account) {
        Objects.requireNonNull(account, "account");
        var entity = new UserAccountJpaEntity();
        entity.setId(account.id().value());
        entity.setEmail(account.email());
        entity.setName(account.name());
        entity.setSurname(account.surname());
        entity.setPictureUrl(account.pictureUrl());
        entity.setPasswordHash(account.passwordHash());
        entity.setBlockedUntil(account.blockedUntil());
        entity.setBlockedReason(account.blockedReason());
        entity.setCreatedAt(account.createdAt());
        entity.setUpdatedAt(account.updatedAt());
        entity.setVersion(account.version());
        entity.setRoles(new java.util.HashSet<>(account.roles()));
        return entity;
    }

    public static UserAccount toDomain(UserAccountJpaEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return new UserAccount(
                new UserId(entity.getId()),
                entity.getEmail(),
                entity.getName(),
                entity.getSurname(),
                entity.getPictureUrl(),
                entity.getPasswordHash(),
                entity.getRoles() == null ? java.util.Set.of() : java.util.Set.copyOf(entity.getRoles()),
                entity.getBlockedUntil(),
                entity.getBlockedReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static void updateEntity(UserAccountJpaEntity entity, UserAccount account) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(account, "account");
        entity.setEmail(account.email());
        entity.setName(account.name());
        entity.setSurname(account.surname());
        entity.setPictureUrl(account.pictureUrl());
        entity.setPasswordHash(account.passwordHash());
        entity.setBlockedUntil(account.blockedUntil());
        entity.setBlockedReason(account.blockedReason());
        entity.setCreatedAt(account.createdAt());
        entity.setUpdatedAt(account.updatedAt());
        entity.setRoles(new java.util.HashSet<>(account.roles()));
    }
}
