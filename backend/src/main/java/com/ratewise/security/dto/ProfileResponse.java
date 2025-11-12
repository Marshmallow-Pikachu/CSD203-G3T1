package com.ratewise.security.dto;


import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ProfileResponse {
    private final String userId;
    private final String username;
    private final String email;
    private final String role;
}
