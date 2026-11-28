package com.pacificaqua.auth;

import com.pacificaqua.auth.AuthDtos.AuthResponse;
import com.pacificaqua.auth.AuthDtos.LoginRequest;
import com.pacificaqua.auth.AuthDtos.UserDto;
import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.UserStatus;
import com.pacificaqua.repo.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwtService;

  public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
    this.users = users;
    this.encoder = encoder;
    this.jwtService = jwtService;
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    User user = users.findByPhone(req.phoneOrEmail().trim())
        .or(() -> users.findByEmailIgnoreCase(req.phoneOrEmail().trim()))
        .orElseThrow(() -> ApiException.unauthorized("Invalid phone/email or password"));
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw ApiException.forbidden("User is inactive");
    }
    if (!encoder.matches(req.password(), user.getPasswordHash())) {
      throw ApiException.unauthorized("Invalid phone/email or password");
    }
    return tokensFor(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse refresh(String refreshToken) {
    try {
      Claims claims = jwtService.parse(refreshToken);
      if (!jwtService.isRefresh(claims)) {
        throw ApiException.unauthorized("Invalid refresh token");
      }
      User user = users.findById(jwtService.userId(claims))
          .orElseThrow(() -> ApiException.unauthorized("User not found"));
      if (user.getStatus() != UserStatus.ACTIVE) {
        throw ApiException.forbidden("User is inactive");
      }
      return tokensFor(user);
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw ApiException.unauthorized("Invalid refresh token");
    }
  }

  public static UserDto toDto(User user) {
    return new UserDto(user.getId(), user.getName(), user.getPhone(), user.getEmail(), user.getRole());
  }

  private AuthResponse tokensFor(User user) {
    return new AuthResponse(jwtService.createAccessToken(user), jwtService.createRefreshToken(user), toDto(user));
  }
}
