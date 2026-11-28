package com.pacificaqua.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "visit_lines")
public class VisitLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "visit_id", nullable = false)
  private Visit visit;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "product_name_snap", nullable = false, length = 120)
  private String productNameSnap;

  @Column(name = "rate_snap", nullable = false, precision = 12, scale = 2)
  private BigDecimal rateSnap;

  @Column(name = "qty_diya", nullable = false)
  private Integer qtyDiya = 0;

  @Column(name = "qty_mila", nullable = false)
  private Integer qtyMila = 0;

  @Column(name = "line_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineAmount = BigDecimal.ZERO;

  public Long getId() { return id; }
  public Visit getVisit() { return visit; }
  public void setVisit(Visit visit) { this.visit = visit; }
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public String getProductNameSnap() { return productNameSnap; }
  public void setProductNameSnap(String productNameSnap) { this.productNameSnap = productNameSnap; }
  public BigDecimal getRateSnap() { return rateSnap; }
  public void setRateSnap(BigDecimal rateSnap) { this.rateSnap = rateSnap; }
  public Integer getQtyDiya() { return qtyDiya; }
  public void setQtyDiya(Integer qtyDiya) { this.qtyDiya = qtyDiya; }
  public Integer getQtyMila() { return qtyMila; }
  public void setQtyMila(Integer qtyMila) { this.qtyMila = qtyMila; }
  public BigDecimal getLineAmount() { return lineAmount; }
  public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }
}
