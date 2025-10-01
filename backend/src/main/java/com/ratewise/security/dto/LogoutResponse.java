package com.ratewise.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LogoutResponse")
public record LogoutResponse(
    @Schema(example = "status message") String status
){}
