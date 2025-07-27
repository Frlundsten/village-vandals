package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.resource.Resource.*;
import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_STARTING_RESOURCE_COUNT;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/** Amount of resources acquired and when it was last updated at db */
@Embeddable
public class ResourceStorage {

  private Instant lastUpdate = Instant.now();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
          name = "resource_storage_resources",
          joinColumns = @JoinColumn(name = "village_id")  // <-- FK referencing owning entity's PK
  )
  @MapKeyColumn(name = "resource")
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "amount")
  private Map<Resource, Integer> resources;

  public ResourceStorage() {
    resources = new EnumMap<>(Resource.class);
    resources.put(FOOD, DEFAULT_STARTING_RESOURCE_COUNT);
    resources.put(BRICKS, DEFAULT_STARTING_RESOURCE_COUNT);
    resources.put(IRON, DEFAULT_STARTING_RESOURCE_COUNT);
    resources.put(WOOD, DEFAULT_STARTING_RESOURCE_COUNT);
  }

  public int get(Resource resource) {
    return resources.get(resource);
  }

  public void set(Resource resource, int amount) {
    resources.put(resource, amount);
  }

  public Instant getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Instant lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  @Override
  public String toString() {
    return "ResourceStorage{"
        + "wood="
        + resources.get(WOOD)
        + ", bricks="
        + resources.get(BRICKS)
        + ", iron="
        + resources.get(IRON)
        + ", food="
        + resources.get(FOOD)
        + ", lastUpdate="
        + lastUpdate
        + '}';
  }
}
