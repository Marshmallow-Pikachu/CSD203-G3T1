package com.ratewise.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleUpdateResponse {
    private final String message;
    private final String userId;
    private final String username;
    private final String email;
    private final String newRole;
    private final String oauthProvider;
    private final String oauthProviderId;
    private final String note;
}
