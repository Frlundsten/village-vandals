package com.villagevandals.vandals.user.dto;

import java.util.UUID;

public interface UserVillageFlatDTO {
    UUID getUserId();
    String getUsername();
    Long getVillageId();
    Integer getX();
    Integer getY();
}