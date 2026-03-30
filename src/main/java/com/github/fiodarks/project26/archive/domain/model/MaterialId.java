package com.github.fiodarks.project26.archive.domain.model;

import java.util.Objects;
import java.util.UUID;

public record MaterialId(UUID value) {
    public MaterialId {
        Objects.requireNonNull(value, "value");
    }
}

