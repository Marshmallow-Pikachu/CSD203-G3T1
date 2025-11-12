package com.ratewise.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;


@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String username;
    private String role;
}