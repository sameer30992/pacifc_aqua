package com.pacificaqua.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "plant_trip_lines")
public class PlantTripLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id", nullable = false)
  private PlantTrip trip;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "qty_lifted", nullable = false)
  private Integer qtyLifted = 0;

  @Column(name = "qty_delivered", nullable = false)
  private Integer qtyDelivered = 0;

  @Column(name = "qty_returned_full", nullable = false)
  private Integer qtyReturnedFull = 0;

  @Column(name = "qty_empties", nullable = false)
  private Integer qtyEmpties = 0;

  @Column(name = "variance_note", length = 500)
  private String varianceNote;

  public Long getId() { return id; }
  public PlantTrip getTrip() { return trip; }
  public void setTrip(PlantTrip trip) { this.trip = trip; }
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public Integer getQtyLifted() { return qtyLifted; }
  public void setQtyLifted(Integer qtyLifted) { this.qtyLifted = qtyLifted; }
  public Integer getQtyDelivered() { return qtyDelivered; }
  public void setQtyDelivered(Integer qtyDelivered) { this.qtyDelivered = qtyDelivered; }
  public Integer getQtyReturnedFull() { return qtyReturnedFull; }
  public void setQtyReturnedFull(Integer qtyReturnedFull) { this.qtyReturnedFull = qtyReturnedFull; }
  public Integer getQtyEmpties() { return qtyEmpties; }
  public void setQtyEmpties(Integer qtyEmpties) { this.qtyEmpties = qtyEmpties; }
  public String getVarianceNote() { return varianceNote; }
  public void setVarianceNote(String varianceNote) { this.varianceNote = varianceNote; }
}
