package com.ratewise.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidateResponse")
public record ValidateResponse(
    @Schema(example = "Token valid for: user@email.com") String message 
){}

