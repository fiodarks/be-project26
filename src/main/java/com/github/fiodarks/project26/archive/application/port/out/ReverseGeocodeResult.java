package com.github.fiodarks.project26.archive.application.port.out;

import java.util.Objects;

public record ReverseGeocodeResult(
        String country,
        String region,
        String city,
        String district,
        String displayName,
        String placeId
) {
    public ReverseGeocodeResult {
        Objects.requireNonNull(displayName, "displayName");
    }
}

