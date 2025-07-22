package com.villagevandals.vandals.user.dto;

import com.villagevandals.vandals.village.dto.VillageDTO;

import java.util.List;
import java.util.UUID;

public record UserDTO (UUID id, String username, List<VillageDTO> villages) {

}
