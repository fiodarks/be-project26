package com.github.fiodarks.project26.users.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.commons.PageResult;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository.spec.UserAccountSpecifications;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.mapper.UserAccountPersistenceMapper;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.entity.UserAccountJpaEntity;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository.UserAccountSpringDataRepository;
import com.github.fiodarks.project26.users.application.port.out.UserAccountSearchCriteria;
import com.github.fiodarks.project26.users.application.port.out.UserAccountRepositoryPort;
import com.github.fiodarks.project26.users.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserAccountRepositoryAdapter implements UserAccountRepositoryPort {

    private final UserAccountSpringDataRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UserId id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id.value()).map(UserAccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String normalizedEmail) {
        Objects.requireNonNull(normalizedEmail, "normalizedEmail");
        return repository.findByEmail(normalizedEmail).map(UserAccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String normalizedEmail) {
        Objects.requireNonNull(normalizedEmail, "normalizedEmail");
        return repository.existsByEmail(normalizedEmail);
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount account) {
        Objects.requireNonNull(account, "account");

        var existing = repository.findById(account.id().value());
        if (existing.isPresent()) {
            var entity = existing.get();
            UserAccountPersistenceMapper.updateEntity(entity, account);
            return UserAccountPersistenceMapper.toDomain(repository.save(entity));
        }
        return UserAccountPersistenceMapper.toDomain(repository.save(UserAccountPersistenceMapper.toEntity(account)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAnyAdmin() {
        return repository.existsAnyWithRole(Role.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserAccount> search(UserAccountSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        Specification<UserAccountJpaEntity> spec = null;
        spec = and(spec, UserAccountSpecifications.freeText(criteria.q()));
        spec = and(spec, UserAccountSpecifications.hasRole(criteria.role()));
        spec = and(spec, UserAccountSpecifications.blocked(criteria.blocked(), criteria.now()));

        var pageRequest = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))
        );

        var page = repository.findAll(spec, pageRequest);
        var items = page.getContent().stream()
                .map(UserAccountPersistenceMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static <T> Specification<T> and(Specification<T> left, Specification<T> right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.and(right);
    }
}
