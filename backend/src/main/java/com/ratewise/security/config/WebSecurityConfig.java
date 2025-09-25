package com.ratewise.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ratewise.security.util.JWTUtil;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final JWTUtil jwtUtil;

    public WebSecurityConfig(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) 
                .cors(cors -> cors.disable()) 
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll() // Login, register, etc.
                        .requestMatchers("/api/v1/health/**").permitAll() // Health check
                        .requestMatchers("/db/**").permitAll()

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/v1/countries/**").authenticated()
                        .requestMatchers("/api/v1/tariffs/**").authenticated()
                        .requestMatchers("/api/**").authenticated() // All other API endpoints

                        // Require authentication for any other request
                        .anyRequest().authenticated())

                // Add JWT filter before the default authentication filter
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {

                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7); // Remove "Bearer " prefix
                        jwtUtil.validateToken(token);

                    } catch (Exception e) {
                        // Token is invalid
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                        return; 
                    }
                }

                filterChain.doFilter(request, response);
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
                String path = request.getRequestURI();

                // Skip JWT validation for public endpoints
                return path.startsWith("/api/v1/auth/") ||
                        path.startsWith("/api/v1/health/") ||
                        path.startsWith("/db/") ||
                        path.equals("/");
            }
        };
    }
}
