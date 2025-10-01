package com.ratewise.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MeResponse")
public record MeResponse(
    @Schema(example = "X") Number userId,
    @Schema(example = "user") String username,
    @Schema(example = "email") String email
){}

