package com.pacificaqua.billing;

import com.pacificaqua.billing.BillingDtos.*;
import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.Visit;
import com.pacificaqua.domain.enums.VisitStatus;
import com.pacificaqua.repo.CustomerRepository;
import com.pacificaqua.repo.VisitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

  private final CustomerRepository customers;
  private final VisitRepository visits;

  @PersistenceContext
  private EntityManager em;

  public BillingService(CustomerRepository customers, VisitRepository visits) {
    this.customers = customers;
    this.visits = visits;
  }

  @Transactional(readOnly = true)
  public CustomerLedger ledger(Long customerId, int year, int month) {
    var customer = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Customer not found"));
    List<Visit> list;
    if (month <= 0) {
      // month=0 → all unpaid (due) visits across months
      list = visits.findDueByCustomerId(customerId);
    } else {
      LocalDate first = LocalDate.of(year, month, 1);
      var from = first.atStartOfDay(ZONE).toInstant();
      var to = first.plusMonths(1).atStartOfDay(ZONE).toInstant();
      list = visits.findByCustomerIdAndStatusAndVisitAtGreaterThanEqualAndVisitAtLessThanOrderByVisitAtAsc(
          customerId, VisitStatus.ACTIVE, from, to);
    }

    BigDecimal totalAmount = BigDecimal.ZERO;
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal totalBalance = BigDecimal.ZERO;
    List<LedgerLine> lines = list.stream().map(v -> {
      Map<String, Integer> given = new LinkedHashMap<>();
      for (var line : v.getLines()) {
        given.merge(line.getProductNameSnap(), line.getQtyDiya(), Integer::sum);
      }
      return new LedgerLine(
          v.getId(), v.getVisitNo(), v.getVisitAt(), given,
          v.getAmountTotal(), v.getAmountPaid(), v.getBalance());
    }).toList();

    for (Visit v : list) {
      totalAmount = totalAmount.add(v.getAmountTotal());
      totalPaid = totalPaid.add(v.getAmountPaid());
      totalBalance = totalBalance.add(v.getBalance());
    }

    return new CustomerLedger(
        customer.getId(),
        customer.getName(),
        customer.getPhone(),
        customer.getCityArea(),
        customer.getCardNo(),
        visits.sumBalanceByCustomer(customerId),
        year,
        month,
        lines,
        totalAmount,
        totalPaid,
        totalBalance);
  }

  @Transactional
  public BillPreview generate(GenerateBillRequest req) {
    if (req.month() <= 0) {
      throw ApiException.badRequest("Pick a calendar month to print or download a bill");
    }
    CustomerLedger ledger = ledger(req.customerId(), req.year(), req.month());
    String ym = String.format("%04d-%02d", req.year(), req.month());
    em.createNativeQuery("""
        INSERT INTO bill_sequences (ym, last_seq) VALUES (?1, 1)
        ON DUPLICATE KEY UPDATE last_seq = last_seq + 1
        """)
        .setParameter(1, ym)
        .executeUpdate();
    Number seq = (Number) em.createNativeQuery("SELECT last_seq FROM bill_sequences WHERE ym = ?1")
        .setParameter(1, ym)
        .getSingleResult();
    String mon = LocalDate.of(req.year(), req.month(), 1)
        .getMonth()
        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        .toUpperCase(Locale.ENGLISH);
    String billNo = mon + "/" + String.format("%03d", seq.intValue());
    return new BillPreview(billNo, ledger, amountInWords(ledger.totalAmount()));
  }

  private static String amountInWords(BigDecimal amount) {
    long rupees = amount == null ? 0 : amount.longValue();
    return "Rupees " + rupees + " only";
  }
}
