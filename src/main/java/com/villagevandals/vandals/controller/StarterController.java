package com.villagevandals.vandals.controller;

import com.villagevandals.vandals.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/register")
public class StarterController {
  RegistrationService registrationService;

  public StarterController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @GetMapping
  public String showRegisterPage() {
    return "register";
  }

  @PostMapping("/user")
  public String register(@RequestParam String username, @RequestParam String password) {
    registrationService.newUser(username, password);
    return "redirect:/login?registered";
  }
}
