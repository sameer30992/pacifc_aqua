package com.pacificaqua.dashboard;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {
  private DashboardDtos() {}

  public record KpiBlock(
      BigDecimal revenue,
      BigDecimal collected,
      BigDecimal pendingDues,
      long visitCount,
      String topProduct,
      long staffPresent) {}

  public record WeekBar(String day, BigDecimal amount) {}

  public record NamedValue(String name, BigDecimal value) {}

  public record RecentSale(
      Long id,
      String visitNo,
      String customerName,
      String helperName,
      BigDecimal amount,
      String status) {}

  public record DashboardSummary(
      KpiBlock kpis,
      List<WeekBar> salesThisWeek,
      List<NamedValue> paymentMix,
      List<NamedValue> productMix,
      List<RecentSale> recentSales) {}
}
