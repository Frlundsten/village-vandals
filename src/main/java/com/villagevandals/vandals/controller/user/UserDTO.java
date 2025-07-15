package com.villagevandals.vandals.controller.user;

import com.villagevandals.vandals.controller.village.VillageDTO;

import java.util.List;
import java.util.UUID;

public record UserDTO (UUID id, String username, List<VillageDTO> villages) {

}
