package com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository;

import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountSpringDataRepository extends JpaRepository<UserAccountJpaEntity, UUID>, JpaSpecificationExecutor<UserAccountJpaEntity> {
    @Query("select count(u) > 0 from UserAccountJpaEntity u join u.roles r where r = :role")
    boolean existsAnyWithRole(@Param("role") Role role);

    Optional<UserAccountJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
