package com.ratewise.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ratewise.security.User;
import com.ratewise.security.entities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JWTUtil {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private final ConcurrentHashMap<Long, String> userTokens = new ConcurrentHashMap<>();

    // Issue JWT token with roles
    public String generateToken(User user) {
        // Check if user already has a valid token
        String existingToken = userTokens.get(user.getId());
        if (existingToken != null) {
            try {
                validateToken(existingToken);
                return existingToken;
            } catch (Exception e) {
                userTokens.remove(user.getId());
            }
        }

        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(1));

        // Convert roles to comma-separated string
        String roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(","));

        String newToken = JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .withClaim("email", user.getEmail())
                .withClaim("roles", roles)
                .sign(Algorithm.HMAC256(secretKey));

        userTokens.put(user.getId(), newToken);

        return newToken;
    }

    // Decode and validate JWT token
    public DecodedJWT validateToken(String token) {
        return JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token);
    }

    // Extract user ID from token
    public Long getUserId(String token) {
        return Long.valueOf(validateToken(token).getSubject());
    }

    // Extract email from token
    public String getEmail(String token) {
        return validateToken(token).getClaim("email").asString();
    }

    // Extract roles from token
    public List<String> getRoles(String token) {
        String rolesString = validateToken(token).getClaim("roles").asString();
        if (rolesString == null || rolesString.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(rolesString.split(","));
    }

    public void invalidateUserToken(Long userId) {
        userTokens.remove(userId);
    }

    public boolean isTokenActiveForUser(String token) {
        try {
            DecodedJWT decoded = validateToken(token);
            Long userId = Long.valueOf(decoded.getSubject());
            String activeToken = userTokens.get(userId);
            return token.equals(activeToken);
        } catch (Exception e) {
            return false;
        }
    }

}