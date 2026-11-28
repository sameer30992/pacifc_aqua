package com.pacificaqua.order;

import com.pacificaqua.domain.enums.OrderStatus;
import com.pacificaqua.order.OrderDtos.*;
import com.pacificaqua.visit.VisitDtos.VisitResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public List<OrderResponse> list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) Long helperId) {
    return service.list(status, date, from, to, customerId, helperId);
  }

  @GetMapping("/mine")
  @PreAuthorize("hasAnyRole('HELPER','OWNER','ADMIN')")
  public List<OrderResponse> mine(@RequestParam(required = false) OrderStatus status) {
    return service.mine(status);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public OrderResponse update(@PathVariable Long id, @Valid @RequestBody CreateOrderRequest request) {
    return service.update(id, request);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public OrderResponse cancel(@PathVariable Long id) {
    return service.cancel(id);
  }

  @PostMapping("/{id}/deliver")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','HELPER')")
  public VisitResponse deliver(@PathVariable Long id, @Valid @RequestBody DeliverOrderRequest request) {
    return service.deliver(id, request);
  }
}
