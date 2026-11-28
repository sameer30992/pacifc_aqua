package com.pacificaqua.domain;

import com.pacificaqua.domain.enums.PlantTripStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plant_trips")
public class PlantTrip {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "helper_id", nullable = false)
  private Long helperId;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private PlantTripStatus status = PlantTripStatus.LOADED;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<PlantTripLine> lines = new ArrayList<>();

  public void addLine(PlantTripLine line) {
    lines.add(line);
    line.setTrip(this);
  }

  public Long getId() { return id; }
  public Long getHelperId() { return helperId; }
  public void setHelperId(Long helperId) { this.helperId = helperId; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getClosedAt() { return closedAt; }
  public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
  public PlantTripStatus getStatus() { return status; }
  public void setStatus(PlantTripStatus status) { this.status = status; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public List<PlantTripLine> getLines() { return lines; }
}
