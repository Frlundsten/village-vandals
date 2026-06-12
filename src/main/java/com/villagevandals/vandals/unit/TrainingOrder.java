package com.villagevandals.vandals.unit;

import com.villagevandals.vandals.village.Village;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "training_order")
public class TrainingOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "village_id", nullable = false)
  private Village village;

  @Column(name = "building_id", nullable = false)
  private Long buildingId;

  @Column(name = "unit_type", nullable = false, length = 31)
  private String unitType;

  @Column(name = "queued_at", nullable = false)
  private Instant queuedAt;

  @Column(name = "finishes_at", nullable = false)
  private Instant finishesAt;

  @Column(name = "completed", nullable = false)
  private boolean completed = false;

  @Column(name = "quantity", nullable = false)
  private int quantity = 1;

  public Long getId() { return id; }

  public Village getVillage() { return village; }
  public void setVillage(Village village) { this.village = village; }

  public Long getBuildingId() { return buildingId; }
  public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }

  public String getUnitType() { return unitType; }
  public void setUnitType(String unitType) { this.unitType = unitType; }

  public Instant getQueuedAt() { return queuedAt; }
  public void setQueuedAt(Instant queuedAt) { this.queuedAt = queuedAt; }

  public Instant getFinishesAt() { return finishesAt; }
  public void setFinishesAt(Instant finishesAt) { this.finishesAt = finishesAt; }

  public boolean isCompleted() { return completed; }
  public void setCompleted(boolean completed) { this.completed = completed; }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
}
