package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.EntityStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40, unique = true)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 60)
  private String category;

  @Column(nullable = false, length = 40)
  private String unit;

  @Column(name = "default_rate", nullable = false, precision = 12, scale = 2)
  private BigDecimal defaultRate = BigDecimal.ZERO;

  @Column(nullable = false)
  private Integer stock = 0;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private EntityStatus status = EntityStatus.ACTIVE;

  @Column(name = "image_path")
  private String imagePath;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void onUpdate() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }
  public BigDecimal getDefaultRate() { return defaultRate; }
  public void setDefaultRate(BigDecimal defaultRate) { this.defaultRate = defaultRate; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public EntityStatus getStatus() { return status; }
  public void setStatus(EntityStatus status) { this.status = status; }
  public String getImagePath() { return imagePath; }
  public void setImagePath(String imagePath) { this.imagePath = imagePath; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
