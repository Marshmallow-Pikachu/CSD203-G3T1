package com.ratewise.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    // Issue JWT token
    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(1));

        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .withClaim("email", email)
                .sign(Algorithm.HMAC256(secretKey));
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
}