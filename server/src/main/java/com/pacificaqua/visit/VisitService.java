package com.pacificaqua.visit;

import com.pacificaqua.auth.SecurityUtils;
import com.pacificaqua.common.ApiException;
import com.pacificaqua.common.PageResponse;
import com.pacificaqua.domain.*;
import com.pacificaqua.domain.enums.PaymentStatus;
import com.pacificaqua.domain.enums.VisitStatus;
import com.pacificaqua.repo.*;
import com.pacificaqua.visit.VisitDtos.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

  private final VisitRepository visits;
  private final PaymentRepository payments;
  private final CustomerRepository customers;
  private final ProductRepository products;
  private final UserRepository users;

  @PersistenceContext
  private EntityManager em;

  public VisitService(
      VisitRepository visits,
      PaymentRepository payments,
      CustomerRepository customers,
      ProductRepository products,
      UserRepository users) {
    this.visits = visits;
    this.payments = payments;
    this.customers = customers;
    this.products = products;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public PageResponse<VisitResponse> list(
      LocalDate from, LocalDate to, String q, PaymentStatus status, String area, Long helperId, int page, int size) {
    Instant fromTs = (from != null ? from : LocalDate.now(ZONE).withDayOfMonth(1)).atStartOfDay(ZONE).toInstant();
    Instant toTs = (to != null ? to.plusDays(1) : LocalDate.now(ZONE).plusDays(1)).atStartOfDay(ZONE).toInstant();
    Page<Visit> result = visits.search(fromTs, toTs, blank(q), status, blank(area), helperId, PageRequest.of(page, size));
    return new PageResponse<>(
        toDtoList(result.getContent()),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional
  public VisitResponse create(CreateVisitRequest req) {
    if (req.clientRequestId() != null && !req.clientRequestId().isBlank()) {
      var existing = visits.findByClientRequestId(req.clientRequestId().trim());
      if (existing.isPresent()) {
        return toDto(existing.get());
      }
    }
    customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer not found"));

    Visit visit = new Visit();
    visit.setVisitNo(nextVisitNo());
    visit.setCustomerId(req.customerId());
    visit.setHelperId(req.helperId());
    visit.setVisitAt(req.visitAt() != null ? req.visitAt() : Instant.now());
    visit.setPaymentMode(req.paymentMode());
    visit.setRemarks(req.remarks());
    visit.setClientRequestId(blank(req.clientRequestId()));
    visit.setCreatedBy(SecurityUtils.requireUser().getId());
    visit.setUpdatedBy(visit.getCreatedBy());

    BigDecimal total = BigDecimal.ZERO;
    for (LineRequest lineReq : req.lines()) {
      if (lineReq.qtyDiya() < 0 || lineReq.qtyMila() < 0) {
        throw ApiException.badRequest("Quantities cannot be negative");
      }
      if (lineReq.qtyDiya() == 0 && lineReq.qtyMila() == 0) {
        throw ApiException.badRequest("Each line needs Given or Empty qty");
      }
      Product product = products.findById(lineReq.productId())
          .orElseThrow(() -> ApiException.notFound("Product not found: " + lineReq.productId()));
      VisitLine line = new VisitLine();
      line.setProductId(product.getId());
      line.setProductNameSnap(product.getName());
      line.setRateSnap(product.getDefaultRate());
      line.setQtyDiya(lineReq.qtyDiya());
      line.setQtyMila(lineReq.qtyMila());
      BigDecimal lineAmount = product.getDefaultRate().multiply(BigDecimal.valueOf(lineReq.qtyDiya()));
      line.setLineAmount(lineAmount);
      total = total.add(lineAmount);
      visit.addLine(line);
      if (lineReq.qtyDiya() > 0) {
        product.setStock(Math.max(0, product.getStock() - lineReq.qtyDiya()));
      }
    }

    BigDecimal paid = req.amountPaid() == null ? BigDecimal.ZERO : req.amountPaid();
    if (paid.compareTo(BigDecimal.ZERO) < 0) throw ApiException.badRequest("Amount paid cannot be negative");
    if (paid.compareTo(total) > 0) throw ApiException.badRequest("Amount paid cannot exceed total");

    visit.setAmountTotal(total);
    visit.setAmountPaid(paid);
    visit.setBalance(total.subtract(paid));
    visit.setPaymentStatus(statusFor(total, paid));

    Visit saved = visits.save(visit);
    if (paid.compareTo(BigDecimal.ZERO) > 0) {
      Payment payment = new Payment();
      payment.setVisitId(saved.getId());
      payment.setAmount(paid);
      payment.setMode(req.paymentMode());
      payment.setPaidAt(saved.getVisitAt());
      payment.setRecordedBy(saved.getCreatedBy());
      payments.save(payment);
    }
    return toDto(saved);
  }

  @Transactional
  public VisitResponse addPayment(Long id, PaymentRequest req) {
    Visit visit = visits.findById(id).orElseThrow(() -> ApiException.notFound("Visit not found"));
    if (visit.getStatus() != VisitStatus.ACTIVE) throw ApiException.badRequest("Visit is void");
    if (req.amount().compareTo(BigDecimal.ZERO) <= 0) throw ApiException.badRequest("Amount must be > 0");
    BigDecimal newPaid = visit.getAmountPaid().add(req.amount());
    if (newPaid.compareTo(visit.getAmountTotal()) > 0) {
      throw ApiException.badRequest("Payment exceeds balance");
    }
    visit.setAmountPaid(newPaid);
    visit.setBalance(visit.getAmountTotal().subtract(newPaid));
    visit.setPaymentStatus(statusFor(visit.getAmountTotal(), newPaid));
    visit.setPaymentMode(req.mode());
    visit.setUpdatedBy(SecurityUtils.requireUser().getId());

    Payment payment = new Payment();
    payment.setVisitId(visit.getId());
    payment.setAmount(req.amount());
    payment.setMode(req.mode());
    payment.setPaidAt(Instant.now());
    payment.setRecordedBy(visit.getUpdatedBy());
    payments.save(payment);
    return toDto(visits.save(visit));
  }

  @Transactional
  public VisitResponse voidVisit(Long id, VoidRequest req) {
    Visit visit = visits.findById(id).orElseThrow(() -> ApiException.notFound("Visit not found"));
    visit.setStatus(VisitStatus.VOID);
    visit.setVoidReason(req.reason());
    visit.setUpdatedBy(SecurityUtils.requireUser().getId());
    return toDto(visits.save(visit));
  }

  private String nextVisitNo() {
    em.createNativeQuery("UPDATE visit_sequences SET last_seq = last_seq + 1 WHERE id = 1").executeUpdate();
    Number seq = (Number) em.createNativeQuery("SELECT last_seq FROM visit_sequences WHERE id = 1").getSingleResult();
    return String.format("PA-%06d", seq.longValue());
  }

  private PaymentStatus statusFor(BigDecimal total, BigDecimal paid) {
    if (paid.compareTo(BigDecimal.ZERO) <= 0) return PaymentStatus.PENDING;
    if (paid.compareTo(total) >= 0) return PaymentStatus.PAID;
    return PaymentStatus.PARTIAL;
  }

  private List<VisitResponse> toDtoList(List<Visit> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }

    Set<Long> customerIds = new HashSet<>();
    Set<Long> helperIds = new HashSet<>();
    for (Visit v : list) {
      if (v.getCustomerId() != null) customerIds.add(v.getCustomerId());
      if (v.getHelperId() != null) helperIds.add(v.getHelperId());
    }

    Map<Long, Customer> customerById = indexById(customers.findAllById(customerIds), Customer::getId);
    Map<Long, User> helperById = indexById(users.findAllById(helperIds), User::getId);

    return list.stream()
        .map(v -> toDto(v, customerById, helperById))
        .toList();
  }

  private VisitResponse toDto(Visit v) {
    return toDtoList(List.of(v)).get(0);
  }

  private VisitResponse toDto(
      Visit v,
      Map<Long, Customer> customerById,
      Map<Long, User> helperById) {
    Customer customer = customerById.get(v.getCustomerId());
    User helper = v.getHelperId() == null ? null : helperById.get(v.getHelperId());
    List<LineResponse> lines = v.getLines().stream()
        .map(l -> new LineResponse(
            l.getProductId(), l.getProductNameSnap(), l.getRateSnap(),
            l.getQtyDiya(), l.getQtyMila(), l.getLineAmount()))
        .toList();
    return new VisitResponse(
        v.getId(),
        v.getVisitNo(),
        v.getCustomerId(),
        customer != null ? customer.getName() : "",
        customer != null ? customer.getPhone() : null,
        customer != null ? customer.getCityArea() : null,
        v.getHelperId(),
        helper != null ? helper.getName() : null,
        v.getVisitAt(),
        v.getPaymentMode(),
        v.getPaymentStatus(),
        v.getAmountTotal(),
        v.getAmountPaid(),
        v.getBalance(),
        v.getRemarks(),
        v.getStatus(),
        lines);
  }

  private static <T> Map<Long, T> indexById(Iterable<T> items, Function<T, Long> idFn) {
    Map<Long, T> map = new HashMap<>();
    for (T item : items) {
      map.put(idFn.apply(item), item);
    }
    return map;
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
