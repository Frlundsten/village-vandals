package com.villagevandals.vandals.controller.login;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogionController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
