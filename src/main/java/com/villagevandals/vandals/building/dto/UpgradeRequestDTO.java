package com.villagevandals.vandals.building.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpgradeRequestDTO(long villageId, long constructionSiteId) {}
