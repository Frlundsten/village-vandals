package com.villagevandals.vandals.web;

import java.io.Serializable;
import java.util.UUID;

public record UserInfo(UUID uuid, String username, String email, String password, String roles)
    implements Serializable {}
