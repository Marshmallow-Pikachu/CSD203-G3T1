package com.ratewise.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ratewise.security.entities.User;
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

    private final ConcurrentHashMap<String, String> userTokens = new ConcurrentHashMap<>();

    // Issue JWT token with role
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

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";

        String newToken = JWT.create()
                .withSubject(user.getId())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withClaim("role", roleName)
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

    // Extract username from token
    public String getUsername(String token) {
        return validateToken(token).getClaim("username").asString();
    }

    // Extract user ID from token
    public String getUserId(String token) {
        return validateToken(token).getSubject();
    }

    // Extract email from token
    public String getEmail(String token) {
        return validateToken(token).getClaim("email").asString();
    }

    // Extract role from token
    public String getRole(String token) {
        String role = validateToken(token).getClaim("role").asString();
        return role != null ? role : "";
    }

    public void invalidateUserToken(String userId) {
        userTokens.remove(userId);
    }

    public boolean isTokenActiveForUser(String token) {
        try {
            DecodedJWT decoded = validateToken(token);
            String userId = decoded.getSubject();
            String activeToken = userTokens.get(userId);
            return token.equals(activeToken);
        } catch (Exception e) {
            return false;
        }
    }

}