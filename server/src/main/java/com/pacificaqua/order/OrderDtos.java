package com.pacificaqua.order;

import com.pacificaqua.domain.enums.OrderPriority;
import com.pacificaqua.domain.enums.OrderStatus;
import com.pacificaqua.domain.enums.PaymentMode;
import com.pacificaqua.visit.VisitDtos.LineRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
  private OrderDtos() {}

  public record OrderLineRequest(@NotNull Long productId, @NotNull Integer qty) {}

  public record CreateOrderRequest(
      @NotNull Long customerId,
      @NotNull Long helperId,
      OrderPriority priority,
      Integer expectedHours,
      String notes,
      @Valid List<OrderLineRequest> lines) {}

  public record DeliverOrderRequest(
      @NotEmpty @Valid List<LineRequest> lines,
      @NotNull BigDecimal amountPaid,
      @NotNull PaymentMode paymentMode,
      String remarks) {}

  public record OrderLineResponse(Long productId, String productName, Integer qty) {}

  public record OrderResponse(
      Long id,
      Long customerId,
      String customerName,
      String customerPhone,
      String customerArea,
      BigDecimal customerDue,
      Long helperId,
      String helperName,
      OrderPriority priority,
      Integer expectedHours,
      String notes,
      OrderStatus status,
      Instant createdAt,
      List<OrderLineResponse> lines) {}

  public record OrderCreatedEvent(
      String type,
      Long orderId,
      Long customerId,
      String customerName,
      OrderPriority priority,
      Integer expectedHours,
      Instant createdAt) {}
}
