package com.pacificaqua.plant;

import com.pacificaqua.plant.PlantDtos.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plant-trips")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class PlantController {
  private final PlantService service;

  public PlantController(PlantService service) {
    this.service = service;
  }

  @GetMapping
  public List<TripResponse> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.list(from, to);
  }

  @GetMapping("/summary")
  public PlantSummary summary() {
    return service.summary();
  }

  @PostMapping
  public TripResponse create(@Valid @RequestBody CreateTripRequest request) {
    return service.create(request);
  }
}
