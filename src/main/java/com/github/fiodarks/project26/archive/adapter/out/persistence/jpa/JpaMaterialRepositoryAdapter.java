package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.MaterialJpaEntity;
import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.mapper.MaterialPersistenceMapper;
import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository.MaterialSpringDataRepository;
import com.github.fiodarks.project26.archive.application.port.out.MaterialHierarchyAggregate;
import com.github.fiodarks.project26.archive.application.port.out.MaterialHierarchyAggregationPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialRepositoryPort;
import com.github.fiodarks.project26.archive.application.port.out.MaterialSearchCriteria;
import com.github.fiodarks.project26.archive.domain.model.GeoBoundingBox;
import com.github.fiodarks.project26.archive.domain.model.HierarchyNodeId;
import com.github.fiodarks.project26.archive.domain.model.Material;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMaterialRepositoryAdapter implements MaterialRepositoryPort, MaterialHierarchyAggregationPort {

    private final MaterialSpringDataRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Material save(Material material) {
        Objects.requireNonNull(material, "material");

        var existing = repository.findById(material.id().value());
        if (existing.isPresent()) {
            var entity = existing.get();
            MaterialPersistenceMapper.updateEntity(entity, material);
            return MaterialPersistenceMapper.toDomain(repository.save(entity));
        }

        return MaterialPersistenceMapper.toDomain(repository.save(MaterialPersistenceMapper.toEntity(material)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Material> findById(MaterialId id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id.value()).map(MaterialPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Material> findByIds(List<MaterialId> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids.stream().map(MaterialId::value).toList())
                .stream()
                .map(MaterialPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(MaterialId id) {
        Objects.requireNonNull(id, "id");
        repository.deleteById(id.value());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Material> search(MaterialSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        var cb = entityManager.getCriteriaBuilder();

        var cq = cb.createQuery(MaterialJpaEntity.class);
        var root = cq.from(MaterialJpaEntity.class);
        var predicates = toPredicates(criteria, cb, cq, root);

        cq.select(root).where(predicates.toArray(Predicate[]::new)).distinct(true);
        cq.orderBy(cb.desc(root.get("createdAt")));

        return entityManager.createQuery(cq)
                .getResultList()
                .stream()
                .map(MaterialPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialHierarchyAggregate> aggregateByHierarchyIdInBbox(GeoBoundingBox bbox) {
        Objects.requireNonNull(bbox, "bbox");

        var cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<MaterialJpaEntity> root = cq.from(MaterialJpaEntity.class);

        var hierarchyIdPath = root.get("hierarchyId");
        var lonPath = root.get("lon").as(Double.class);
        var latPath = root.get("lat").as(Double.class);

        cq.multiselect(
                hierarchyIdPath,
                cb.count(root),
                cb.min(lonPath),
                cb.min(latPath),
                cb.max(lonPath),
                cb.max(latPath)
        );

        cq.where(
                cb.isNotNull(latPath),
                cb.isNotNull(lonPath),
                cb.between(latPath, bbox.minLat(), bbox.maxLat()),
                cb.between(lonPath, bbox.minLon(), bbox.maxLon())
        );

        cq.groupBy(hierarchyIdPath);

        return entityManager.createQuery(cq)
                .getResultList()
                .stream()
                .map(t -> new MaterialHierarchyAggregate(
                        new HierarchyNodeId(t.get(0, java.util.UUID.class)),
                        t.get(1, Long.class),
                        new GeoBoundingBox(
                                t.get(2, Double.class),
                                t.get(3, Double.class),
                                t.get(4, Double.class),
                                t.get(5, Double.class)
                        )
                ))
                .toList();
    }

    private static List<Predicate> toPredicates(
            MaterialSearchCriteria criteria,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<MaterialJpaEntity> root
    ) {
        var predicates = new ArrayList<Predicate>();

        if (criteria.location() != null && !criteria.location().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(root.get("location")),
                    "%" + criteria.location().toLowerCase(Locale.ROOT) + "%"
            ));
        }

        if (criteria.placeId() != null && !criteria.placeId().isBlank()) {
            predicates.add(cb.equal(root.get("placeId"), criteria.placeId()));
        }

        if (criteria.createdBy() != null) {
            predicates.add(cb.equal(root.get("createdBy"), criteria.createdBy().value()));
        }

        if (criteria.hierarchyIds() != null && !criteria.hierarchyIds().isEmpty()) {
            predicates.add(root.get("hierarchyId").in(criteria.hierarchyIds().stream().map(id -> id.value()).toList()));
        }

        if (criteria.searchPhrase() != null && !criteria.searchPhrase().isBlank()) {
            var phrase = "%" + criteria.searchPhrase().toLowerCase(Locale.ROOT) + "%";

            var tagsJoin = root.join("tags", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), phrase),
                    cb.like(cb.lower(root.get("description")), phrase),
                    cb.like(cb.lower(root.get("location")), phrase),
                    cb.like(cb.lower(tagsJoin.as(String.class)), phrase)
            ));
        }

        var dateFrom = criteria.dateFrom();
        if (dateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("creationDateTo"), dateFrom));
        }

        var dateTo = criteria.dateTo();
        if (dateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("creationDateFrom"), dateTo));
        }

        if (criteria.bbox() != null) {
            predicates.add(cb.between(root.get("lat"), criteria.bbox().minLat(), criteria.bbox().maxLat()));
            predicates.add(cb.between(root.get("lon"), criteria.bbox().minLon(), criteria.bbox().maxLon()));
        }

        if (criteria.tags() != null && !criteria.tags().isEmpty()) {
            var tagsJoin = root.join("tags", JoinType.INNER);
            predicates.add(tagsJoin.in(criteria.tags()));
        }

        if (criteria.filter() != null && !criteria.filter().isEmpty()) {
            for (var entry : criteria.filter().entrySet()) {
                MapJoin<MaterialJpaEntity, String, String> metadataJoin = root.joinMap("metadata", JoinType.INNER);
                predicates.add(cb.and(
                        cb.equal(metadataJoin.key(), entry.getKey()),
                        cb.equal(metadataJoin.value(), entry.getValue())
                ));
            }
        }

        return predicates;
    }
}
