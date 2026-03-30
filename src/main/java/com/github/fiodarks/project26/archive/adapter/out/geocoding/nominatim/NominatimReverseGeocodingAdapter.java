package com.github.fiodarks.project26.archive.adapter.out.geocoding.nominatim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fiodarks.project26.archive.application.port.out.ReverseGeocodeResult;
import com.github.fiodarks.project26.archive.application.port.out.ReverseGeocodingPort;
import com.github.fiodarks.project26.archive.domain.model.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NominatimReverseGeocodingAdapter implements ReverseGeocodingPort {

    private final NominatimGeocodingProperties properties;

    @Override
    public Optional<ReverseGeocodeResult> reverse(GeoPoint point) {
        if (point == null) {
            throw new IllegalArgumentException("point must not be null");
        }

        var client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        NominatimReverseResponse response;
        try {
            response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", Double.toString(point.lat()))
                            .queryParam("lon", Double.toString(point.lon()))
                            .queryParam("zoom", "18")
                            .queryParam("addressdetails", "1")
                            .build())
                    .retrieve()
                    .body(NominatimReverseResponse.class);
        } catch (RestClientResponseException e) {
            return Optional.empty();
        }

        if (response == null || response.displayName() == null || response.displayName().isBlank()) {
            return Optional.empty();
        }

        var address = response.address();
        String country = address == null ? null : blankToNull(address.country());
        String region = address == null ? null : firstNonBlank(address.state(), address.region(), address.stateDistrict());
        String city = address == null ? null : firstNonBlank(address.city(), address.town(), address.village(), address.municipality());
        String district = address == null ? null : firstNonBlank(address.borough(), address.cityDistrict(), address.suburb(), address.neighbourhood());

        return Optional.of(new ReverseGeocodeResult(
                country,
                region,
                city,
                district,
                response.displayName(),
                response.placeId() == null ? null : response.placeId().toString()
        ));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (var v : values) {
            var normalized = blankToNull(v);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimReverseResponse(
            @JsonProperty("place_id") Long placeId,
            @JsonProperty("display_name") String displayName,
            NominatimAddress address
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimAddress(
            String country,
            String state,
            String region,
            @JsonProperty("state_district") String stateDistrict,
            String city,
            String town,
            String village,
            String municipality,
            String borough,
            @JsonProperty("city_district") String cityDistrict,
            String suburb,
            String neighbourhood
    ) {}
}

