package com.pacificaqua.plant;

import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.PlantTrip;
import com.pacificaqua.domain.PlantTripLine;
import com.pacificaqua.domain.Product;
import com.pacificaqua.domain.enums.PlantTripStatus;
import com.pacificaqua.plant.PlantDtos.*;
import com.pacificaqua.repo.PlantTripRepository;
import com.pacificaqua.repo.ProductRepository;
import com.pacificaqua.repo.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

  private final PlantTripRepository trips;
  private final ProductRepository products;
  private final UserRepository users;

  public PlantService(PlantTripRepository trips, ProductRepository products, UserRepository users) {
    this.trips = trips;
    this.products = products;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public List<TripResponse> list(LocalDate from, LocalDate to) {
    Instant fromTs = from == null ? null : from.atStartOfDay(ZONE).toInstant();
    Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();
    return trips.search(fromTs, toTs).stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public PlantSummary summary() {
    int jarInPlant = products.findAll().stream()
        .filter(p -> "jar".equalsIgnoreCase(p.getCode()) || p.getName().toLowerCase().contains("20 l"))
        .mapToInt(Product::getStock)
        .sum();
    LocalDate today = LocalDate.now(ZONE);
    Instant from = today.atStartOfDay(ZONE).toInstant();
    Instant to = today.plusDays(1).atStartOfDay(ZONE).toInstant();
    int lifted = 0;
    int delivered = 0;
    int empties = 0;
    for (PlantTrip trip : trips.search(from, to)) {
      for (PlantTripLine line : trip.getLines()) {
        lifted += line.getQtyLifted();
        delivered += line.getQtyDelivered();
        empties += line.getQtyEmpties();
      }
    }
    return new PlantSummary(jarInPlant, lifted, delivered, empties);
  }

  @Transactional
  public TripResponse create(CreateTripRequest req) {
    users.findById(req.helperId()).orElseThrow(() -> ApiException.notFound("Helper not found"));
    PlantTrip trip = new PlantTrip();
    trip.setHelperId(req.helperId());
    trip.setStartedAt(req.startedAt() != null ? req.startedAt() : Instant.now());
    trip.setStatus(PlantTripStatus.LOADED);
    trip.setNotes(req.notes());
    for (TripLineRequest lineReq : req.lines()) {
      Product product = products.findById(lineReq.productId())
          .orElseThrow(() -> ApiException.notFound("Product not found"));
      PlantTripLine line = new PlantTripLine();
      line.setProductId(product.getId());
      line.setQtyLifted(lineReq.qtyLifted());
      line.setQtyDelivered(lineReq.qtyDelivered() == null ? 0 : lineReq.qtyDelivered());
      line.setQtyReturnedFull(lineReq.qtyReturnedFull() == null ? 0 : lineReq.qtyReturnedFull());
      line.setQtyEmpties(lineReq.qtyEmpties() == null ? 0 : lineReq.qtyEmpties());
      line.setVarianceNote(lineReq.varianceNote());
      trip.addLine(line);
      product.setStock(Math.max(0, product.getStock() - lineReq.qtyLifted()));
    }
    return toDto(trips.save(trip));
  }

  private TripResponse toDto(PlantTrip t) {
    String helperName = users.findById(t.getHelperId()).map(u -> u.getName()).orElse("");
    List<TripLineResponse> lines = t.getLines().stream().map(l -> {
      String name = products.findById(l.getProductId()).map(Product::getName).orElse("?");
      return new TripLineResponse(
          l.getProductId(), name, l.getQtyLifted(), l.getQtyDelivered(),
          l.getQtyReturnedFull(), l.getQtyEmpties(), l.getVarianceNote());
    }).toList();
    return new TripResponse(
        t.getId(), t.getHelperId(), helperName, t.getStartedAt(), t.getClosedAt(),
        t.getStatus(), t.getNotes(), lines);
  }
}
