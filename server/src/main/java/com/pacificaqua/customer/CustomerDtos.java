package com.pacificaqua.customer;

import com.pacificaqua.domain.enums.CustomerType;
import com.pacificaqua.domain.enums.EntityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class CustomerDtos {
  private CustomerDtos() {}

  public record CustomerSyncMeta(
      Instant customersRevision,
      Instant balancesRevision,
      long activeCount) {}

  public record CustomerResponse(
      Long id,
      String name,
      String phone,
      String cityArea,
      CustomerType type,
      String cardNo,
      String notes,
      Long routeHelperId,
      String avatarPath,
      EntityStatus status,
      BigDecimal outstanding) {}

  public record CustomerRequest(
      @NotBlank String name,
      String phone,
      String cityArea,
      @NotNull CustomerType type,
      String cardNo,
      String notes,
      Long routeHelperId,
      String avatarPath) {}
}
