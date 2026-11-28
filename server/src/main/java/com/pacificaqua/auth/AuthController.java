package com.pacificaqua.auth;

import com.pacificaqua.auth.AuthDtos.LoginRequest;
import com.pacificaqua.auth.AuthDtos.SessionResponse;
import com.pacificaqua.auth.AuthDtos.UserDto;
import com.pacificaqua.common.ApiException;
import com.pacificaqua.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;
  private final UserRepository users;
  private final AuthCookieService cookies;

  public AuthController(AuthService authService, UserRepository users, AuthCookieService cookies) {
    this.authService = authService;
    this.users = users;
    this.cookies = cookies;
  }

  @PostMapping("/login")
  public SessionResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletResponse response) {
    var auth = authService.login(request);
    cookies.writeTokens(response, auth.accessToken(), auth.refreshToken());
    return new SessionResponse(auth.user());
  }

  @PostMapping("/refresh")
  public SessionResponse refresh(HttpServletRequest request, HttpServletResponse response) {
    String refresh = cookies.read(request, AuthCookieService.REFRESH_COOKIE);
    if (refresh == null || refresh.isBlank()) {
      throw ApiException.unauthorized("Missing refresh session");
    }
    var auth = authService.refresh(refresh);
    cookies.writeTokens(response, auth.accessToken(), auth.refreshToken());
    return new SessionResponse(auth.user());
  }

  @PostMapping("/logout")
  public Map<String, String> logout(HttpServletResponse response) {
    cookies.clear(response);
    return Map.of("status", "ok");
  }

  @GetMapping("/me")
  public UserDto me() {
    UserPrincipal principal = SecurityUtils.requireUser();
    return users.findById(principal.getId())
        .map(AuthService::toDto)
        .orElseThrow();
  }
}
