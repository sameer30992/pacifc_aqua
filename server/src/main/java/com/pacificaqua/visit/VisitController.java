package com.pacificaqua.visit;

import com.pacificaqua.common.PageResponse;
import com.pacificaqua.domain.enums.PaymentStatus;
import com.pacificaqua.visit.VisitDtos.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/visits")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','HELPER')")
public class VisitController {
  private final VisitService service;

  public VisitController(VisitService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<VisitResponse> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) PaymentStatus status,
      @RequestParam(required = false) String area,
      @RequestParam(required = false) Long helperId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(from, to, q, status, area, helperId, page, size);
  }

  @PostMapping
  public VisitResponse create(@Valid @RequestBody CreateVisitRequest request) {
    return service.create(request);
  }

  @PostMapping("/{id}/payments")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public VisitResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
    return service.addPayment(id, request);
  }

  @PostMapping("/{id}/void")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public VisitResponse voidVisit(@PathVariable Long id, @Valid @RequestBody VoidRequest request) {
    return service.voidVisit(id, request);
  }
}
