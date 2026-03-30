package com.github.fiodarks.project26.archive.domain.model;

public record GeoPoint(double lat, double lon) {
    public GeoPoint {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat out of range: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("lon out of range: " + lon);
        }
    }
}

