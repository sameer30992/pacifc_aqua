package com.pacificaqua.visit;

import com.pacificaqua.domain.enums.PaymentMode;
import com.pacificaqua.domain.enums.PaymentStatus;
import com.pacificaqua.domain.enums.VisitStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class VisitDtos {
  private VisitDtos() {}

  public record LineRequest(
      @NotNull Long productId,
      @NotNull Integer qtyDiya,
      @NotNull Integer qtyMila) {}

  public record CreateVisitRequest(
      @NotNull Long customerId,
      Long helperId,
      Instant visitAt,
      @NotEmpty @Valid List<LineRequest> lines,
      @NotNull BigDecimal amountPaid,
      @NotNull PaymentMode paymentMode,
      String remarks,
      String clientRequestId) {}

  public record PaymentRequest(
      @NotNull BigDecimal amount,
      @NotNull PaymentMode mode) {}

  public record VoidRequest(@NotNull String reason) {}

  public record LineResponse(
      Long productId,
      String productName,
      BigDecimal rate,
      Integer qtyDiya,
      Integer qtyMila,
      BigDecimal lineAmount) {}

  public record VisitResponse(
      Long id,
      String visitNo,
      Long customerId,
      String customerName,
      String customerPhone,
      String customerArea,
      Long helperId,
      String helperName,
      Instant visitAt,
      PaymentMode paymentMode,
      PaymentStatus paymentStatus,
      BigDecimal amountTotal,
      BigDecimal amountPaid,
      BigDecimal balance,
      String remarks,
      VisitStatus status,
      List<LineResponse> lines) {}
}
