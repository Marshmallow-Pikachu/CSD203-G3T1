package com.ratewise.security.dto;


import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ProfileResponse {
    private final Long userId;
    private final String username;
    private final String email;
}
