package com.villagevandals.vandals.controller.user;

import java.util.List;

public record UserDTO (long id, String username, List<com.villagevandals.vandals.controller.village.VillageDTO> villages) {

}
