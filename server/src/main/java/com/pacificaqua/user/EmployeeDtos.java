package com.pacificaqua.user;

import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class EmployeeDtos {
  private EmployeeDtos() {}

  public record EmployeeResponse(
      Long id,
      String name,
      String phone,
      String email,
      UserRole role,
      UserStatus status,
      String avatarPath,
      boolean appAccess) {}

  public record EmployeeRequest(
      @NotBlank String name,
      @NotBlank String phone,
      String email,
      @NotNull UserRole role,
      String password,
      String avatarPath) {}
}
