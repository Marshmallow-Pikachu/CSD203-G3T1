package com.ratewise.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public WebSecurityConfig(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

        @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // for swagger UI
                        .requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                        ).permitAll()                
                        .requestMatchers("/error").permitAll() 
                        // Public authentication endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/registration").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/session").permitAll()
                        // Protected authentication endpoints (any authenticated user)

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/session").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/profile").hasAnyRole("ADMIN", "USER")
                         // Admin-only authentication endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/validation").hasRole("ADMIN")

                        .requestMatchers("/api/v1/health/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/db/**").hasAnyRole("ADMIN", "USER")

                        // Admin-only endpoints
                        .requestMatchers("/api/v1/countries/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/tariffs/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")

                        /* Additional Endpoints once countries and tariff endpoints are up
                         * .requestMatchers(HttpMethod.POST, "/countries").hasRole("ADMIN") // only admins can add entries into country table
                         * .requestMatchers(HttpMethod.DELETE, "/countries").hasRole("ADMIN") // only admins can delete entries from country table
                         * .requestMatchers(HttpMethod.PUT, "/countries").hasRole("ADMIN") // only admins can update country table
                         * .requestMatchers(HttpMethod.GET, "/countries/**").hasAnyRole("ADMIN", "USER") // users and admin can view entries of the country table
                         * .requestMatchers(HttpMethod.GET, "/tariffs/**").hasAnyRole("ADMIN", "USER")  // users and admin can view entries of the tariff table
                         * .requestMatchers(HttpMethod.POST, "/tariffs").hasRole("ADMIN") // only admins can add entries into tariff table
                         * .requestMatchers(HttpMethod.DELETE, "/tariffs").hasRole("ADMIN") // only admins can delete entries from tariff table
                         * .requestMatchers(HttpMethod.PUT, "/tariffs").hasRole("ADMIN") // only admins can update tariff table
                         */

                        // Require authentication for any other request
                        .anyRequest().hasRole("ADMIN"))
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
                // Never authenticate preflight
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);

                        jwtUtil.validateToken(token);

                        Long userId = jwtUtil.getUserId(token);
                        String email = jwtUtil.getEmail(token);
                        List<String> roles = jwtUtil.getRoles(token);

                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

                        User user = userOpt.get();
                        if (!user.isEnabled()) throw new RuntimeException("User account is disabled");

                        // Convert roles to Spring Security authorities
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();

                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                            email, null, authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                    } catch (Exception e) {
                        SecurityContextHolder.clearContext();
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid or expired token: " + e.getMessage() + "\"}");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
            }

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                String method = request.getMethod();

                // Skip JWT validation for SwaggerUI
                if (path.startsWith("/v3/api-docs/") ||
                    path.startsWith("/swagger-ui/") ||
                    path.equals("/swagger-ui.html")) {
                    return true;
                }

                // Skip JWT validation for public authentication endpoints only
                if (path.equals("/api/v1/auth/registration") && "POST".equals(method)) {
                    return true;
                }
                if (path.equals("/api/v1/auth/session") && "POST".equals(method)) {
                    return true;
                }

                // Skip for root path
                return path.equals("/");
            }
        };
    }
}