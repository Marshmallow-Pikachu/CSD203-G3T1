package com.ratewise.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ratewise.security.util.JWTUtil;
import com.ratewise.security.UserRepository;
import com.ratewise.security.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public WebSecurityConfig(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
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
                        // for swagger UI
                        .requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                        ).permitAll()                
                        // Public endpoints (no authentication required)
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/health/**").permitAll()
                        .requestMatchers("/db/**").permitAll()

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/v1/countries/**").authenticated()
                        .requestMatchers("/api/v1/tariffs/**").authenticated()
                        .requestMatchers("/api/**").authenticated()

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
                        String token = authHeader.substring(7);
                        
                        // Validate the token
                        jwtUtil.validateToken(token);
                        
                        // Extract user information from token
                        Long userId = jwtUtil.getUserId(token);
                        String email = jwtUtil.getEmail(token);
                        
                        // Verify user exists and is active
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) {
                            throw new RuntimeException("User not found");
                        }
                        
                        User user = userOpt.get();
                        if (!user.isEnabled()) {
                            throw new RuntimeException("User account is disabled");
                        }
                        
                        // Create and set authentication
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        
                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                    } catch (Exception e) {
                        // Clear any existing authentication
                        SecurityContextHolder.clearContext();
                        
                        // Token is invalid - set error response
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid or expired token: " + e.getMessage() + "\"}");
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
                String path = request.getRequestURI();

                // Skip JWT validation for SwaggerUI
                if (path.startsWith("/v3/api-docs/") ||
                    path.startsWith("/swagger-ui/") ||
                    path.equals("/swagger-ui.html")) {
                    return true;
                }

                // Skip JWT validation for public endpoints
                return path.startsWith("/api/v1/auth/") ||
                        path.startsWith("/api/v1/health/") ||
                        path.startsWith("/db/") ||
                        path.equals("/");
            }
        };
    }
}
