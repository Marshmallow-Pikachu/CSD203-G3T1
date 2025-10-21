package com.ratewise.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class TokenValidationResponse {
    private final String message;
    private final String email;
    private final boolean valid;
}
