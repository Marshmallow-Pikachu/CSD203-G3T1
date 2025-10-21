package com.ratewise.security.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserSummaryResponse {
    private final String userId;
    private final String username;
    private final String email;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final String role;
}
