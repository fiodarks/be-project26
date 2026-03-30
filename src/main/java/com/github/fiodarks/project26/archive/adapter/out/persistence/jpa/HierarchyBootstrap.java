package com.github.fiodarks.project26.archive.adapter.out.persistence.jpa;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.HierarchyNodeJpaEntity;
import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository.HierarchyNodeSpringDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HierarchyBootstrap implements ApplicationRunner {

    private final HierarchyNodeSpringDataRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        var rootId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var countryId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        var regionId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        var cityId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var districtId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        repository.saveAll(List.of(
                node(rootId, null, 0, "Archive", "Top-level archive root"),
                node(countryId, rootId, 1, "Poland", "Example country"),
                node(regionId, countryId, 2, "Mazowieckie", "Example region"),
                node(cityId, regionId, 3, "Warsaw", "Example city"),
                node(districtId, cityId, 4, "Śródmieście", "Example district")
        ));
    }

    private static HierarchyNodeJpaEntity node(UUID id, UUID parentId, int level, String name, String description) {
        var entity = new HierarchyNodeJpaEntity();
        entity.setId(id);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setName(name);
        entity.setDescription(description);
        entity.setStatus("APPROVED");
        return entity;
    }
}
