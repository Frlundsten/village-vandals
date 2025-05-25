package com.villagevandals.vandals.model.domain;

import java.util.List;

public record User(String username, String passwordHash, List<Village> villages) {
}
