package com.pacificaqua.dashboard;

import com.pacificaqua.dashboard.DashboardDtos.*;
import com.pacificaqua.domain.Visit;
import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import com.pacificaqua.domain.enums.VisitStatus;
import com.pacificaqua.repo.CustomerRepository;
import com.pacificaqua.repo.UserRepository;
import com.pacificaqua.repo.VisitRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

  private final VisitRepository visits;
  private final UserRepository users;
  private final CustomerRepository customers;

  public DashboardService(VisitRepository visits, UserRepository users, CustomerRepository customers) {
    this.visits = visits;
    this.users = users;
    this.customers = customers;
  }

  @Transactional(readOnly = true)
  public DashboardSummary summary(String month) {
    LocalDate first = month != null && month.matches("\\d{4}-\\d{2}")
        ? LocalDate.parse(month + "-01")
        : LocalDate.now(ZONE).withDayOfMonth(1);
    var from = first.atStartOfDay(ZONE).toInstant();
    var to = first.plusMonths(1).atStartOfDay(ZONE).toInstant();

    BigDecimal revenue = visits.sumRevenue(from, to);
    BigDecimal collected = visits.sumCollected(from, to);
    BigDecimal pending = visits.sumAllPending();
    long visitCount = visits.countByStatusAndVisitAtGreaterThanEqualAndVisitAtLessThan(VisitStatus.ACTIVE, from, to);
    long staff = users.search(null, UserRole.HELPER, UserStatus.ACTIVE).size();

    List<Visit> recent = visits.findTop8ByStatusOrderByVisitAtDesc(VisitStatus.ACTIVE);
    List<RecentSale> enriched = new ArrayList<>();
    for (Visit v : recent) {
      String customerName = customers.findById(v.getCustomerId()).map(c -> c.getName()).orElse("Customer");
      String helperName = v.getHelperId() == null
          ? "—"
          : users.findById(v.getHelperId()).map(u -> u.getName()).orElse("Helper");
      enriched.add(new RecentSale(
          v.getId(),
          v.getVisitNo(),
          customerName,
          helperName,
          v.getAmountTotal(),
          v.getPaymentStatus().name()));
    }

    LocalDate today = LocalDate.now(ZONE);
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    List<WeekBar> week = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      LocalDate d = weekStart.plusDays(i);
      var df = d.atStartOfDay(ZONE).toInstant();
      var dt = d.plusDays(1).atStartOfDay(ZONE).toInstant();
      week.add(new WeekBar(
          d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
          visits.sumRevenue(df, dt)));
    }

    Map<String, BigDecimal> payMix = new HashMap<>();
    Map<String, BigDecimal> prodMix = new HashMap<>();
    for (Visit v : visits.search(from, to, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 500))) {
      payMix.merge(v.getPaymentStatus().name(), v.getAmountTotal(), BigDecimal::add);
      for (var line : v.getLines()) {
        prodMix.merge(line.getProductNameSnap(), line.getLineAmount(), BigDecimal::add);
      }
    }

    String topProduct = prodMix.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("—");

    KpiBlock kpis = new KpiBlock(revenue, collected, pending, visitCount, topProduct, staff);
    List<NamedValue> paymentMix = payMix.entrySet().stream()
        .map(e -> new NamedValue(e.getKey(), e.getValue())).toList();
    List<NamedValue> productMix = prodMix.entrySet().stream()
        .map(e -> new NamedValue(e.getKey(), e.getValue())).toList();

    return new DashboardSummary(kpis, week, paymentMix, productMix, enriched);
  }
}
