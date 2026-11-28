package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.PaymentMode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "visit_id", nullable = false)
  private Long visitId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private PaymentMode mode;

  @Column(name = "paid_at", nullable = false)
  private Instant paidAt;

  @Column(name = "recorded_by")
  private Long recordedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Long getVisitId() { return visitId; }
  public void setVisitId(Long visitId) { this.visitId = visitId; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public PaymentMode getMode() { return mode; }
  public void setMode(PaymentMode mode) { this.mode = mode; }
  public Instant getPaidAt() { return paidAt; }
  public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
  public Long getRecordedBy() { return recordedBy; }
  public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
  public Instant getCreatedAt() { return createdAt; }
}
