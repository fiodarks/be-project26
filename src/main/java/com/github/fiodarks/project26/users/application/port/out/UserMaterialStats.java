package com.github.fiodarks.project26.users.application.port.out;

import java.time.OffsetDateTime;

public record UserMaterialStats(long materialsCount, OffsetDateTime lastMaterialCreatedAt) {
}

