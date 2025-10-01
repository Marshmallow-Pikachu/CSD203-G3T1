package com.ratewise.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterResponse", description = "Response after user registration")
public record RegisterResponse(
    @Schema(example = "true", description = "Indicates whether the registration was successful")
    boolean ok,

    @Schema(example = "User registered successfully", description = "Result message")
    String message
) {}
