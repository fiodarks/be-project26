package com.github.fiodarks.project26.auth;

import com.github.fiodarks.project26.adapter.in.web.dto.GoogleIdTokenRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.TokenResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.UserRole;
import com.github.fiodarks.project26.config.ArchiveSecurityProperties;
import com.github.fiodarks.project26.config.GoogleOAuthProperties;
import com.github.fiodarks.project26.archive.application.exception.ValidationException;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.application.port.in.UserProfile;
import com.github.fiodarks.project26.users.application.service.UserAccountApplicationService;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public final class GoogleOAuthService {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);

    private final Clock clock;
    private final GoogleOAuthProperties googleProperties;
    private final AuthStateService stateService;
    private final RestClient restClient;
    private final JwtEncoder internalJwtEncoder;
    private final NimbusJwtDecoder googleIdTokenDecoder;
    private final UserAccountApplicationService users;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public GoogleOAuthService(
            Clock clock,
            GoogleOAuthProperties googleProperties,
            ArchiveSecurityProperties jwtProperties,
            JwtEncoder internalJwtEncoder,
            UserAccountApplicationService users
    ) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (googleProperties == null) {
            throw new IllegalArgumentException("googleProperties must not be null");
        }
        if (jwtProperties == null) {
            throw new IllegalArgumentException("jwtProperties must not be null");
        }
        if (internalJwtEncoder == null) {
            throw new IllegalArgumentException("internalJwtEncoder must not be null");
        }
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        this.clock = clock;
        this.googleProperties = googleProperties;
        this.stateService = new AuthStateService(clock, jwtProperties);
        this.restClient = RestClient.builder().build();
        this.internalJwtEncoder = internalJwtEncoder;
        this.googleIdTokenDecoder = buildGoogleIdTokenDecoder(googleProperties);
        this.users = users;
    }

    public URI buildLoginRedirectUri() {
        requireEnabled();
        String state = stateService.newState();

        String query = "client_id=" + urlEncode(googleProperties.clientId())
                + "&redirect_uri=" + urlEncode(googleProperties.redirectUri())
                + "&response_type=code"
                + "&scope=" + urlEncode("openid email profile")
                + "&state=" + urlEncode(state)
                + "&prompt=" + urlEncode("consent");

        return URI.create(AUTH_ENDPOINT + "?" + query);
    }

    public TokenResponse handleCallback(String code, String state) {
        requireEnabled();
        if (code == null || code.isBlank()) {
            throw new ValidationException("code is required");
        }
        stateService.validate(state);

        String idToken = exchangeCodeForIdToken(code);
        return exchangeGoogleIdToken(new GoogleIdTokenRequest(idToken));
    }

    public TokenResponse exchangeGoogleIdToken(GoogleIdTokenRequest request) {
        requireEnabled();
        if (request == null) {
            throw new ValidationException("request is required");
        }
        if (request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new ValidationException("idToken is required");
        }

        Jwt googleJwt;
        try {
            googleJwt = googleIdTokenDecoder.decode(request.getIdToken());
        } catch (Exception e) {
            throw new org.springframework.security.core.AuthenticationException("Invalid Google id_token") {};
        }

        String googleSub = googleJwt.getSubject();
        if (googleSub == null || googleSub.isBlank()) {
            throw new ValidationException("Google id_token missing subject");
        }

        UUID internalUserId = UUID.nameUUIDFromBytes(("google:" + googleSub).getBytes(StandardCharsets.UTF_8));

        var givenName = toNullableString(googleJwt.getClaims().get("given_name"));
        var familyName = toNullableString(googleJwt.getClaims().get("family_name"));
        var fullName = toNullableString(googleJwt.getClaims().get("name"));

        var profile = new UserProfile(
                toNullableString(googleJwt.getClaims().get("email")),
                givenName != null ? givenName : fullName,
                familyName != null ? familyName : guessSurnameFromFullName(fullName),
                toNullableString(googleJwt.getClaims().get("picture"))
        );
        var roles = users.resolveRolesForLogin(new com.github.fiodarks.project26.archive.domain.model.UserId(internalUserId), profile);

        String accessToken = issueInternalJwt(internalUserId, roles, googleJwt.getClaims());

        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds((int) ACCESS_TOKEN_TTL.toSeconds());
        response.setUserId(internalUserId);
        response.setRoles(roles.stream().map(r -> UserRole.valueOf(r.name())).collect(Collectors.toList()));
        return response;
    }

    public void requireEnabled() {
        if (!googleProperties.enabled()) {
            throw new ValidationException("Google OAuth is disabled. Set archive.security.google.enabled=true and configure client-id/client-secret/redirect-uri.");
        }
    }

    private String exchangeCodeForIdToken(String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("code", code);
        form.add("client_id", googleProperties.clientId());
        form.add("client_secret", googleProperties.clientSecret());
        form.add("redirect_uri", googleProperties.redirectUri());
        form.add("grant_type", "authorization_code");

        Map<String, Object> payload;
        try {
            payload = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            String body = truncateForClient(e.getResponseBodyAsString(), 800);
            throw new ValidationException("Failed to exchange Google authorization code: HTTP " + e.getStatusCode().value() + " " + body);
        } catch (Exception e) {
            throw new ValidationException("Failed to exchange Google authorization code");
        }

        if (payload == null || !payload.containsKey("id_token")) {
            throw new ValidationException("Google token endpoint did not return id_token");
        }
        Object idToken = payload.get("id_token");
        if (!(idToken instanceof String s) || s.isBlank()) {
            throw new ValidationException("Google token endpoint returned invalid id_token");
        }
        return s;
    }

    private static String truncateForClient(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String s = value.trim();
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "...";
    }

    private String issueInternalJwt(UUID userId, Set<Role> roles, Map<String, Object> googleClaims) {
        Instant now = Instant.now(clock);
        var builder = JwtClaimsSet.builder()
                .issuer("project26")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .subject(userId.toString())
                .claim("roles", roles.stream().map(Role::name).toList())
                ;

        Object email = googleClaims.get("email");
        if (email != null) {
            builder.claim("email", email);
        }
        Object name = googleClaims.get("name");
        if (name != null) {
            builder.claim("name", name);
        }
        Object picture = googleClaims.get("picture");
        if (picture != null) {
            builder.claim("picture", picture);
        }

        var claims = builder.build();

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return internalJwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private static NimbusJwtDecoder buildGoogleIdTokenDecoder(GoogleOAuthProperties properties) {
        if (!properties.enabled()) {
            return null;
        }

        var decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();

        var issuerHttps = JwtValidators.createDefaultWithIssuer("https://accounts.google.com");
        var issuerNoScheme = JwtValidators.createDefaultWithIssuer("accounts.google.com");
        var issuerValidator = new MultiIssuerJwtValidator(List.of(issuerHttps, issuerNoScheme));

        var validator = new DelegatingOAuth2TokenValidator<>(
                issuerValidator,
                new GoogleAudienceValidator(properties.clientId())
        );

        decoder.setJwtValidator(validator);
        return decoder;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String toNullableString(Object value) {
        if (value == null) {
            return null;
        }
        var s = value.toString().trim();
        return s.isBlank() ? null : s;
    }

    private static String guessSurnameFromFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        var normalized = fullName.trim().replaceAll("\\s+", " ");
        var idx = normalized.lastIndexOf(' ');
        if (idx < 0 || idx == normalized.length() - 1) {
            return null;
        }
        var surname = normalized.substring(idx + 1).trim();
        return surname.isBlank() ? null : surname;
    }
}
