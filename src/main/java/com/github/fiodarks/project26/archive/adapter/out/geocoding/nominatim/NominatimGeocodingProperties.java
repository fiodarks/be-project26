package com.github.fiodarks.project26.archive.adapter.out.geocoding.nominatim;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archive.geocoding.nominatim")
public record NominatimGeocodingProperties(
        String baseUrl,
        String userAgent
) {
    public NominatimGeocodingProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://nominatim.openstreetmap.org";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Project26/1.0";
        }
    }
}

