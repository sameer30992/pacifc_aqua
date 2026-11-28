package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.CustomerType;
import com.pacificaqua.domain.enums.EntityStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 20)
  private String phone;

  @Column(name = "city_area", length = 120)
  private String cityArea;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private CustomerType type = CustomerType.RETAIL;

  @Column(name = "card_no", length = 40)
  private String cardNo;

  @Column(length = 500)
  private String notes;

  @Column(name = "route_helper_id")
  private Long routeHelperId;

  @Column(name = "avatar_path")
  private String avatarPath;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private EntityStatus status = EntityStatus.ACTIVE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void onUpdate() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getCityArea() { return cityArea; }
  public void setCityArea(String cityArea) { this.cityArea = cityArea; }
  public CustomerType getType() { return type; }
  public void setType(CustomerType type) { this.type = type; }
  public String getCardNo() { return cardNo; }
  public void setCardNo(String cardNo) { this.cardNo = cardNo; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Long getRouteHelperId() { return routeHelperId; }
  public void setRouteHelperId(Long routeHelperId) { this.routeHelperId = routeHelperId; }
  public String getAvatarPath() { return avatarPath; }
  public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
  public EntityStatus getStatus() { return status; }
  public void setStatus(EntityStatus status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
