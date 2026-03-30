package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.GeoPoint;

import java.util.Optional;

public interface ReverseGeocodingPort {
    Optional<ReverseGeocodeResult> reverse(GeoPoint point);
}

