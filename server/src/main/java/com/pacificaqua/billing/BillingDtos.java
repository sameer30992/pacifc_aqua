package com.pacificaqua.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class BillingDtos {
  private BillingDtos() {}

  public record LedgerLine(
      Long visitId,
      String visitNo,
      Instant visitAt,
      Map<String, Integer> givenByProduct,
      BigDecimal amount,
      BigDecimal paid,
      BigDecimal balance) {}

  public record CustomerLedger(
      Long customerId,
      String customerName,
      String phone,
      String address,
      String cardNo,
      BigDecimal due,
      int year,
      int month,
      List<LedgerLine> lines,
      BigDecimal totalAmount,
      BigDecimal totalPaid,
      BigDecimal totalBalance) {}

  public record GenerateBillRequest(Long customerId, int year, int month) {}

  public record BillPreview(
      String billNo,
      CustomerLedger ledger,
      String amountInWords) {}
}
