package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.Material;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;

import java.util.List;
import java.util.Optional;

public interface MaterialRepositoryPort {
    Material save(Material material);

    Optional<Material> findById(MaterialId id);

    List<Material> findByIds(List<MaterialId> ids);

    void deleteById(MaterialId id);

    List<Material> search(MaterialSearchCriteria criteria);
}
