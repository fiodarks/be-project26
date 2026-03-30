package com.github.fiodarks.project26.archive.application.port.in;

import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;

public interface DeleteMaterialUseCase {
    void delete(Actor actor, MaterialId id);
}
