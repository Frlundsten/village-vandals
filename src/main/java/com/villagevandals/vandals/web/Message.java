package com.villagevandals.vandals.web;

import java.util.Map;

public record Message(Map<String, String> message) {
    public static Message of(String info) {
        return new Message(Map.of("message", info));
    }
}
