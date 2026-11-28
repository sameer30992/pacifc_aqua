package com.pacificaqua.user;

import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import com.pacificaqua.repo.UserRepository;
import com.pacificaqua.user.EmployeeDtos.EmployeeRequest;
import com.pacificaqua.user.EmployeeDtos.EmployeeResponse;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
  private final UserRepository users;
  private final PasswordEncoder encoder;

  public EmployeeService(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @Transactional(readOnly = true)
  public List<EmployeeResponse> list(String q, UserRole role, UserStatus status) {
    return users.search(blankToNull(q), role, status).stream().map(this::toDto).toList();
  }

  @Transactional
  public EmployeeResponse create(EmployeeRequest req) {
    if (users.existsByPhone(req.phone().trim())) {
      throw ApiException.conflict("Phone already registered");
    }
    if (req.email() != null && !req.email().isBlank() && users.existsByEmailIgnoreCase(req.email().trim())) {
      throw ApiException.conflict("Email already registered");
    }
    User user = new User();
    apply(user, req);
    String raw = (req.password() == null || req.password().isBlank()) ? "changeme123" : req.password();
    user.setPasswordHash(encoder.encode(raw));
    user.setStatus(UserStatus.ACTIVE);
    return toDto(users.save(user));
  }

  @Transactional
  public EmployeeResponse update(Long id, EmployeeRequest req) {
    User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
    users.findByPhone(req.phone().trim()).ifPresent(other -> {
      if (!other.getId().equals(id)) throw ApiException.conflict("Phone already registered");
    });
    if (req.email() != null && !req.email().isBlank()) {
      users.findByEmailIgnoreCase(req.email().trim()).ifPresent(other -> {
        if (!other.getId().equals(id)) throw ApiException.conflict("Email already registered");
      });
    }
    apply(user, req);
    if (req.password() != null && !req.password().isBlank()) {
      user.setPasswordHash(encoder.encode(req.password()));
    }
    return toDto(users.save(user));
  }

  @Transactional
  public EmployeeResponse setStatus(Long id, UserStatus status) {
    User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
    user.setStatus(status);
    return toDto(users.save(user));
  }

  private void apply(User user, EmployeeRequest req) {
    user.setName(req.name().trim());
    user.setPhone(req.phone().trim());
    user.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim());
    user.setRole(req.role());
    user.setAvatarPath(req.avatarPath());
  }

  private EmployeeResponse toDto(User u) {
    boolean access = u.getStatus() == UserStatus.ACTIVE;
    return new EmployeeResponse(
        u.getId(), u.getName(), u.getPhone(), u.getEmail(), u.getRole(), u.getStatus(), u.getAvatarPath(), access);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
