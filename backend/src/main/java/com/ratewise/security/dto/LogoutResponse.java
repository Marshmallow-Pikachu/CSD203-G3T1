package com.ratewise.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutResponse {
    private final String message;
    private final Long userId;
}
