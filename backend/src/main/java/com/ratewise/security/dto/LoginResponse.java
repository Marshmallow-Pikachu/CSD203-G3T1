package com.ratewise.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;


@Data
@Builder
public class LoginResponse {
    private final String accessToken;
}
