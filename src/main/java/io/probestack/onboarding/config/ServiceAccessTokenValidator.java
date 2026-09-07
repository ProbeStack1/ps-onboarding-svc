package io.probestack.onboarding.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.security.authn.model.AuthnToken;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ServiceAccessTokenValidator {
    public static final String SERVICE_TOKEN_TYPE = "probestack_service_access";

    private final ServiceAccessTokenValidationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;
    private volatile KeyCache cache = new KeyCache(Map.of(), Instant.EPOCH);

    public ServiceAccessTokenValidator(
            ServiceAccessTokenValidationProperties properties,
            ObjectMapper objectMapper) {
        this(properties, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                        .build(),
                Clock.systemUTC());
    }

    ServiceAccessTokenValidator(
            ServiceAccessTokenValidationProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            Clock clock) {
        validateProperties(properties);
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public boolean canHandle(String encodedToken) {
        try {
            DecodedJWT decoded = JWT.decode(encodedToken);
            return properties.getIssuer().equals(decoded.getIssuer())
                    && SERVICE_TOKEN_TYPE.equals(decoded.getClaim("token_type").asString());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public AuthnToken verify(String encodedToken) {
        DecodedJWT unverified = JWT.decode(encodedToken);
        if (!"RS256".equals(unverified.getAlgorithm()) || !StringUtils.hasText(unverified.getKeyId())) {
            throw new IllegalArgumentException("Service access token must use RS256 and contain kid");
        }
        RSAPublicKey publicKey = resolveKey(unverified.getKeyId());
        DecodedJWT verified = JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(properties.getIssuer())
                .withAudience(properties.getAudience())
                .withClaim("token_type", SERVICE_TOKEN_TYPE)
                .withClaim("principal_type", "SERVICE")
                .build()
                .verify(encodedToken);
        if (!StringUtils.hasText(verified.getClaim("client_id").asString())
                || !StringUtils.hasText(verified.getClaim("organization_id").asString())) {
            throw new IllegalArgumentException("Service access token is missing required claims");
        }
        return new AuthnToken(verified);
    }

    private RSAPublicKey resolveKey(String keyId) {
        KeyCache current = cache;
        if (clock.instant().isBefore(current.expiresAt()) && current.keys().containsKey(keyId)) {
            return current.keys().get(keyId);
        }
        synchronized (this) {
            current = cache;
            if (clock.instant().isBefore(current.expiresAt()) && current.keys().containsKey(keyId)) {
                return current.keys().get(keyId);
            }
            Map<String, RSAPublicKey> keys = fetchKeys();
            cache = new KeyCache(keys, clock.instant().plus(
                    Duration.ofMinutes(Math.max(1, properties.getJwksCacheDurationMinutes()))));
            RSAPublicKey key = keys.get(keyId);
            if (key == null) throw new IllegalArgumentException("No service-token signing key found for kid=" + keyId);
            return key;
        }
    }

    private Map<String, RSAPublicKey> fetchKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getJwksUri()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("JWKS endpoint returned HTTP " + response.statusCode());
            }
            JsonNode keysNode = objectMapper.readTree(response.body()).path("keys");
            if (!keysNode.isArray()) throw new IllegalStateException("JWKS response has no keys array");
            Map<String, RSAPublicKey> keys = new HashMap<>();
            for (JsonNode keyNode : keysNode) {
                if (!"RSA".equals(keyNode.path("kty").asText())
                        || !"RS256".equals(keyNode.path("alg").asText())
                        || !StringUtils.hasText(keyNode.path("kid").asText())) continue;
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.path("e").asText()));
                RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                keys.put(keyNode.path("kid").asText(), key);
            }
            if (keys.isEmpty()) throw new IllegalStateException("JWKS endpoint returned no usable RS256 keys");
            return Map.copyOf(keys);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading service-token JWKS", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load service-token JWKS", ex);
        }
    }

    private static void validateProperties(ServiceAccessTokenValidationProperties properties) {
        if (!StringUtils.hasText(properties.getIssuer())
                || !StringUtils.hasText(properties.getAudience())
                || !StringUtils.hasText(properties.getJwksUri())) {
            throw new IllegalStateException("service-token issuer, audience and jwks-uri are required");
        }
    }

    private record KeyCache(Map<String, RSAPublicKey> keys, Instant expiresAt) { }
}
