package com.pacificaqua.plant;

import com.pacificaqua.domain.enums.PlantTripStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class PlantDtos {
  private PlantDtos() {}

  public record TripLineRequest(
      @NotNull Long productId,
      @NotNull Integer qtyLifted,
      Integer qtyDelivered,
      Integer qtyReturnedFull,
      Integer qtyEmpties,
      String varianceNote) {}

  public record CreateTripRequest(
      @NotNull Long helperId,
      Instant startedAt,
      String notes,
      @NotEmpty @Valid List<TripLineRequest> lines) {}

  public record TripLineResponse(
      Long productId,
      String productName,
      Integer qtyLifted,
      Integer qtyDelivered,
      Integer qtyReturnedFull,
      Integer qtyEmpties,
      String varianceNote) {}

  public record TripResponse(
      Long id,
      Long helperId,
      String helperName,
      Instant startedAt,
      Instant closedAt,
      PlantTripStatus status,
      String notes,
      List<TripLineResponse> lines) {}

  public record PlantSummary(
      int jarInPlant,
      int liftedToday,
      int deliveredToday,
      int emptyReturned) {}
}
