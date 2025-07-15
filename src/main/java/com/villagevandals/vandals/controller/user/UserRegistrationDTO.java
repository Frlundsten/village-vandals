package com.villagevandals.vandals.controller.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegistrationDTO(
    @NotBlank(message = "Username") String username,
    @Email(message = "Email") @NotBlank String email,
    @NotBlank(message = "Password") String password) {
  public UserRegistrationDTO {
    username = username == null ? null : username.trim();
    email = email == null ? null : email.trim();
    password = password == null ? null : password.trim();
  }
}
