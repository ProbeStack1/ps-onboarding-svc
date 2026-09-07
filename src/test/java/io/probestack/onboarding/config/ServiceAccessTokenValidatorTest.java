package io.probestack.onboarding.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceAccessTokenValidatorTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void validatesIssuerSignatureAudienceTypeAndRequiredClaimsFromRemoteJwks() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();
        String jwks = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"key-1\",\"alg\":\"RS256\","
                + "\"n\":\"" + unsigned(publicKey.getModulus()) + "\",\"e\":\"" + unsigned(publicKey.getPublicExponent()) + "\"}]}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ServiceAccessTokenValidationProperties properties = new ServiceAccessTokenValidationProperties();
        properties.setIssuer("https://issuer.test/token-issuer-api");
        properties.setAudience("probestack-api");
        properties.setJwksUri("http://localhost:" + server.getAddress().getPort() + "/jwks");
        ServiceAccessTokenValidator validator = new ServiceAccessTokenValidator(properties, new ObjectMapper());
        Instant now = Instant.now();
        String valid = JWT.create().withKeyId("key-1")
                .withIssuer(properties.getIssuer()).withAudience(properties.getAudience())
                .withSubject("service:probestack-admin-backend")
                .withClaim("client_id", "probestack-admin-backend")
                .withClaim("organization_id", "org-1")
                .withClaim("principal_type", "SERVICE")
                .withClaim("token_type", ServiceAccessTokenValidator.SERVICE_TOKEN_TYPE)
                .withArrayClaim("scope", new String[]{"onboarding:members:read"})
                .withIssuedAt(Date.from(now)).withNotBefore(Date.from(now.minusSeconds(1)))
                .withExpiresAt(Date.from(now.plus(Duration.ofMinutes(5))))
                .sign(Algorithm.RSA256(publicKey, (RSAPrivateKey) pair.getPrivate()));

        assertThat(validator.canHandle(valid)).isTrue();
        assertThat(validator.verify(valid)).isNotNull();

        String wrongAudience = JWT.create().withKeyId("key-1")
                .withIssuer(properties.getIssuer()).withAudience("other-api")
                .withClaim("client_id", "probestack-admin-backend").withClaim("organization_id", "org-1")
                .withClaim("principal_type", "SERVICE")
                .withClaim("token_type", ServiceAccessTokenValidator.SERVICE_TOKEN_TYPE)
                .withExpiresAt(Date.from(now.plusSeconds(300)))
                .sign(Algorithm.RSA256(publicKey, (RSAPrivateKey) pair.getPrivate()));
        assertThatThrownBy(() -> validator.verify(wrongAudience)).isInstanceOf(RuntimeException.class);
    }

    private static String unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
