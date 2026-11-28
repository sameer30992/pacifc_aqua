package com.pacificaqua.auth;

import com.pacificaqua.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
  private AuthDtos() {}

  public record LoginRequest(
      @NotBlank String phoneOrEmail,
      @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record UserDto(Long id, String name, String phone, String email, UserRole role) {}

  public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}

  /** Browser session payload — tokens stay in HttpOnly cookies only. */
  public record SessionResponse(UserDto user) {}
}

