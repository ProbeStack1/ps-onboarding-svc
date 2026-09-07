package io.probestack.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.service-token-validation")
public class ServiceAccessTokenValidationProperties {
    private boolean enabled;
    private String issuer;
    private String audience;
    private String jwksUri;
    private long jwksCacheDurationMinutes = 15;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getJwksUri() { return jwksUri; }
    public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
    public long getJwksCacheDurationMinutes() { return jwksCacheDurationMinutes; }
    public void setJwksCacheDurationMinutes(long value) { this.jwksCacheDurationMinutes = value; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
