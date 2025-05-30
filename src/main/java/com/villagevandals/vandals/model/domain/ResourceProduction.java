package com.villagevandals.vandals.model.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceProduction {
  private int woodPerHour = 3600;
  private int clayPerHour = 3600;
  private int ironPerHour = 3600;
  private int cropPerHour = 3600;
}
