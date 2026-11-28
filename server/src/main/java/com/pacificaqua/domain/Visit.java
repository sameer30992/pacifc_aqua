package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.PaymentMode;
import com.pacificaqua.domain.enums.PaymentStatus;
import com.pacificaqua.domain.enums.VisitStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "visits")
public class Visit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "visit_no", nullable = false, unique = true, length = 20)
  private String visitNo;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "helper_id")
  private Long helperId;

  @Column(name = "visit_at", nullable = false)
  private Instant visitAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "payment_mode", nullable = false, length = 20)
  private PaymentMode paymentMode = PaymentMode.CASH;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  @Column(name = "amount_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountTotal = BigDecimal.ZERO;

  @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountPaid = BigDecimal.ZERO;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(length = 500)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private VisitStatus status = VisitStatus.ACTIVE;

  @Column(name = "void_reason", length = 500)
  private String voidReason;

  @Column(name = "client_request_id", unique = true, length = 64)
  private String clientRequestId;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<VisitLine> lines = new ArrayList<>();

  @PreUpdate
  void onUpdate() { updatedAt = Instant.now(); }

  public void addLine(VisitLine line) {
    lines.add(line);
    line.setVisit(this);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getVisitNo() { return visitNo; }
  public void setVisitNo(String visitNo) { this.visitNo = visitNo; }
  public Long getCustomerId() { return customerId; }
  public void setCustomerId(Long customerId) { this.customerId = customerId; }
  public Long getHelperId() { return helperId; }
  public void setHelperId(Long helperId) { this.helperId = helperId; }
  public Instant getVisitAt() { return visitAt; }
  public void setVisitAt(Instant visitAt) { this.visitAt = visitAt; }
  public PaymentMode getPaymentMode() { return paymentMode; }
  public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
  public BigDecimal getAmountTotal() { return amountTotal; }
  public void setAmountTotal(BigDecimal amountTotal) { this.amountTotal = amountTotal; }
  public BigDecimal getAmountPaid() { return amountPaid; }
  public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }
  public String getRemarks() { return remarks; }
  public void setRemarks(String remarks) { this.remarks = remarks; }
  public VisitStatus getStatus() { return status; }
  public void setStatus(VisitStatus status) { this.status = status; }
  public String getVoidReason() { return voidReason; }
  public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
  public String getClientRequestId() { return clientRequestId; }
  public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<VisitLine> getLines() { return lines; }
}
