package com.pacificaqua.product;

import com.pacificaqua.domain.enums.EntityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ProductDtos {
  private ProductDtos() {}

  public record ProductResponse(
      Long id,
      String code,
      String name,
      String category,
      String unit,
      BigDecimal defaultRate,
      Integer stock,
      EntityStatus status,
      String imagePath) {}

  public record ProductRequest(
      String code,
      @NotBlank String name,
      @NotBlank String category,
      @NotBlank String unit,
      @NotNull BigDecimal defaultRate,
      Integer stock,
      EntityStatus status,
      String imagePath) {}

  public record StockPatch(@NotNull Integer stock) {}
}
