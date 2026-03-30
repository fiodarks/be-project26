package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.Material;

public interface UpdateMaterialUseCase {
    Material update(UpdateMaterialRequest request);
}

