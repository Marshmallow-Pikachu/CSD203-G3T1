
package com.ratewise.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class SecurityExceptionHandler {

    private Map<String, Object> err(String title, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", title);
        if (msg != null && !msg.isBlank()) m.put("hint", msg);
        return m;
    }

    /** Bean validation on controller method params/bodies (e.g., @Valid DTOs). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = (first != null)
                ? first.getField() + ": " + first.getDefaultMessage()
                : "Invalid request payload.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(err("Invalid input.", msg));
    }

    /** Unauthenticated/failed login (e.g., wrong password, expired token mapped to AuthenticationException). */
    @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
    public ResponseEntity<?> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(err("Authentication failed.", ex.getMessage()));
    }

    /** Authenticated but not allowed (Spring Security decision). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(err("Access denied.", ex.getMessage()));
    }

    // IMPORTANT: Do NOT catch RuntimeException or Exception here,
    // to avoid swallowing business errors that should be handled by the API handler.
}
