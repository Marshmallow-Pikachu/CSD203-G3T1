package com.ratewise.security.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TokenValidationResponse {
    private final String message;
    private final String email;
    private final boolean valid;
}
