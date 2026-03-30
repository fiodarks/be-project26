package com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.repository;

import com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.entity.AuditEventJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditEventSpringDataRepository extends JpaRepository<AuditEventJpaEntity, UUID> {

    @Query("""
            select e from AuditEventJpaEntity e
            where (:actorUserId is null or e.actorUserId = :actorUserId)
              and (:targetUserId is null or e.targetUserId = :targetUserId)
              and (:action is null or e.action = :action)
              and (:from is null or e.at >= :from)
              and (:to is null or e.at <= :to)
            order by e.at desc
            """)
    Page<AuditEventJpaEntity> search(
            @Param("actorUserId") UUID actorUserId,
            @Param("targetUserId") UUID targetUserId,
            @Param("action") String action,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );
}

