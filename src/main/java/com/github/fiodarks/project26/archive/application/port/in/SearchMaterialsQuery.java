package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.Material;

import java.util.List;

public interface SearchMaterialsQuery {
    List<Material> search(SearchMaterialsRequest request);
}
