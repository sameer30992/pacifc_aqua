package com.pacificaqua.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private final Jwt jwt = new Jwt();
  private final Cors cors = new Cors();

  public Jwt getJwt() { return jwt; }
  public Cors getCors() { return cors; }

  public static class Jwt {
    private String secret;
    private long accessTokenMinutes = 480;
    private long refreshTokenDays = 30;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenMinutes() { return accessTokenMinutes; }
    public void setAccessTokenMinutes(long accessTokenMinutes) { this.accessTokenMinutes = accessTokenMinutes; }
    public long getRefreshTokenDays() { return refreshTokenDays; }
    public void setRefreshTokenDays(long refreshTokenDays) { this.refreshTokenDays = refreshTokenDays; }
  }

  public static class Cors {
    private String allowedOrigins = "http://localhost:5173";

    public String getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
  }
}
