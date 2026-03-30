package com.github.fiodarks.project26.archive.domain.model;

import java.util.Objects;

public record GeoBoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
    public GeoBoundingBox {
        if (minLat < -90.0 || minLat > 90.0) {
            throw new IllegalArgumentException("minLat out of range: " + minLat);
        }
        if (maxLat < -90.0 || maxLat > 90.0) {
            throw new IllegalArgumentException("maxLat out of range: " + maxLat);
        }
        if (minLon < -180.0 || minLon > 180.0) {
            throw new IllegalArgumentException("minLon out of range: " + minLon);
        }
        if (maxLon < -180.0 || maxLon > 180.0) {
            throw new IllegalArgumentException("maxLon out of range: " + maxLon);
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat must be <= maxLat");
        }
        if (minLon > maxLon) {
            throw new IllegalArgumentException("minLon must be <= maxLon");
        }
    }

    public static GeoBoundingBox fromBboxDoubles(Iterable<Double> bbox) {
        Objects.requireNonNull(bbox, "bbox");
        var values = new java.util.ArrayList<Double>(4);
        for (var v : bbox) {
            values.add(v);
        }
        if (values.size() != 4) {
            throw new IllegalArgumentException("bbox must have exactly 4 numbers (minLon,minLat,maxLon,maxLat)");
        }
        return new GeoBoundingBox(values.get(0), values.get(1), values.get(2), values.get(3));
    }
}
