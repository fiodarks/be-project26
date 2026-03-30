package com.github.fiodarks.project26.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class GoogleAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public GoogleAudienceValidator(String expectedAudience) {
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalArgumentException("expectedAudience must be set");
        }
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audiences = token.getAudience();
        if (audiences != null && audiences.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "Google id_token audience (aud) mismatch",
                null
        ));
    }
}

