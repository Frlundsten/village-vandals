package com.villagevandals.vandals.building.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConstructionRequestDTO(String type, long constructionSiteId, long villageId) {}
