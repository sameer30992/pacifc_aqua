package com.pacificaqua.auth;

import com.pacificaqua.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
  public static final String ACCESS_COOKIE = "pa_access";
  public static final String REFRESH_COOKIE = "pa_refresh";

  private final AppProperties props;

  public AuthCookieService(AppProperties props) {
    this.props = props;
  }

  public void writeTokens(HttpServletResponse response, String accessToken, String refreshToken) {
    long accessMaxAge = props.getJwt().getAccessTokenMinutes() * 60;
    long refreshMaxAge = props.getJwt().getRefreshTokenDays() * 24 * 3600;
    add(response, ACCESS_COOKIE, accessToken, accessMaxAge);
    add(response, REFRESH_COOKIE, refreshToken, refreshMaxAge);
  }

  public void clear(HttpServletResponse response) {
    add(response, ACCESS_COOKIE, "", 0);
    add(response, REFRESH_COOKIE, "", 0);
  }

  public String read(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        String v = cookie.getValue();
        return (v == null || v.isBlank()) ? null : v;
      }
    }
    return null;
  }

  private void add(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
    ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
        .httpOnly(true)
        .secure(false) // local HTTP shop LAN; enable when HTTPS is on
        .path("/")
        .sameSite("Lax")
        .maxAge(maxAgeSeconds)
        .build();
    response.addHeader("Set-Cookie", cookie.toString());
  }
}
