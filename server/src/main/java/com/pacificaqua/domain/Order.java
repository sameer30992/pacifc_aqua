package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.OrderPriority;
import com.pacificaqua.domain.enums.OrderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "helper_id", nullable = false)
  private Long helperId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private OrderPriority priority = OrderPriority.NORMAL;

  @Column(name = "expected_hours")
  private Integer expectedHours;

  @Column(length = 500)
  private String notes;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<OrderLine> lines = new ArrayList<>();

  @PreUpdate
  void onUpdate() { updatedAt = Instant.now(); }

  public void addLine(OrderLine line) {
    lines.add(line);
    line.setOrder(this);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getCustomerId() { return customerId; }
  public void setCustomerId(Long customerId) { this.customerId = customerId; }
  public Long getHelperId() { return helperId; }
  public void setHelperId(Long helperId) { this.helperId = helperId; }
  public OrderPriority getPriority() { return priority; }
  public void setPriority(OrderPriority priority) { this.priority = priority; }
  public Integer getExpectedHours() { return expectedHours; }
  public void setExpectedHours(Integer expectedHours) { this.expectedHours = expectedHours; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public OrderStatus getStatus() { return status; }
  public void setStatus(OrderStatus status) { this.status = status; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<OrderLine> getLines() { return lines; }
}
