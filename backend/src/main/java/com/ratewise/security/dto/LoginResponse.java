package com.ratewise.security.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginResponse", description = "Response returned after successful login")
public class LoginResponse {

    @Schema(example = "true", description = "Indicates whether login was successful")
    private boolean ok;

    @Schema(example = "Login successful", description = "Message describing the login outcome")
    private String message;

    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", 
            description = "JWT access token for authentication")
    private String accessToken;
}
