package com.ratewise.security.dto;

import lombok.Getter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginRequest", description = "Credentials used to authenticate a user")
public class LoginRequest {

    @NotBlank(message = "Username is required. Please enter a valid username")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(example = "marc123", description = "Username of the account")
    private String username;

    @NotBlank(message = "Password is required. Please enter your password")
    @Schema(example = "StrongPass123", description = "Account password")
    private String password;
}
