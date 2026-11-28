package com.pacificaqua.auth;

import com.pacificaqua.config.AppProperties;
import com.pacificaqua.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final AppProperties props;
  private final SecretKey key;

  public JwtService(AppProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(User user) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getJwt().getAccessTokenMinutes() * 60);
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("name", user.getName())
        .claim("role", user.getRole().name())
        .claim("typ", "access")
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public String createRefreshToken(User user) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(props.getJwt().getRefreshTokenDays() * 24 * 3600);
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("typ", "refresh")
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public Long userId(Claims claims) {
    return Long.valueOf(claims.getSubject());
  }

  public boolean isAccess(Claims claims) {
    return "access".equals(claims.get("typ", String.class));
  }

  public boolean isRefresh(Claims claims) {
    return "refresh".equals(claims.get("typ", String.class));
  }
}
