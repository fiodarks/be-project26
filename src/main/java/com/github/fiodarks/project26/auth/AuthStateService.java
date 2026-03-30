package com.github.fiodarks.project26.auth;

import com.github.fiodarks.project26.config.ArchiveSecurityProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class AuthStateService {
    private static final Duration DEFAULT_MAX_AGE = Duration.ofMinutes(10);
    private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();

    private final Clock clock;
    private final SecretKeySpec key;
    private final Duration maxAge;

    public AuthStateService(Clock clock, ArchiveSecurityProperties jwtProperties) {
        this(clock, jwtProperties, DEFAULT_MAX_AGE);
    }

    public AuthStateService(Clock clock, ArchiveSecurityProperties jwtProperties, Duration maxAge) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (jwtProperties == null) {
            throw new IllegalArgumentException("jwtProperties must not be null");
        }
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
        this.clock = clock;
        this.key = new SecretKeySpec(jwtProperties.hmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.maxAge = maxAge;
    }

    public String newState() {
        String payload = Instant.now(clock).getEpochSecond() + ":" + UUID.randomUUID();
        byte[] signature = hmacSha256(payload.getBytes(StandardCharsets.UTF_8));
        return B64_URL.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + B64_URL.encodeToString(signature);
    }

    public void validate(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state is required");
        }
        String[] parts = state.split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("state has invalid format");
        }

        byte[] payloadBytes;
        byte[] signatureBytes;
        try {
            payloadBytes = B64_URL_DEC.decode(parts[0]);
            signatureBytes = B64_URL_DEC.decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("state has invalid base64url encoding");
        }

        byte[] expected = hmacSha256(payloadBytes);
        if (!MessageDigest.isEqual(expected, signatureBytes)) {
            throw new IllegalArgumentException("state signature invalid");
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] payloadParts = payload.split(":", 2);
        if (payloadParts.length != 2) {
            throw new IllegalArgumentException("state payload invalid");
        }

        long issuedAtEpochSeconds;
        try {
            issuedAtEpochSeconds = Long.parseLong(payloadParts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("state timestamp invalid");
        }

        Instant issuedAt = Instant.ofEpochSecond(issuedAtEpochSeconds);
        Instant now = Instant.now(clock);
        if (issuedAt.isAfter(now.plusSeconds(30))) {
            throw new IllegalArgumentException("state timestamp invalid");
        }
        if (issuedAt.isBefore(now.minus(maxAge))) {
            throw new IllegalArgumentException("state expired");
        }
    }

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }
}

