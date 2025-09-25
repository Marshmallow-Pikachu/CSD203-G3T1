package com.ratewise.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/health/**").permitAll()
                        .requestMatchers("/db/**").permitAll()

                        // Protected endpoints (authentication required)
                        .requestMatchers("/api/v1/auth/validate").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/countries/**").authenticated()
                        .requestMatchers("/api/v1/tariffs/**").authenticated()
                        .requestMatchers("/api/**").authenticated()

                        // Require authentication for any other request
                        .anyRequest().authenticated())
            // ✅ JWT filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    // ✅ Central CORS config used by Spring Security
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // React dev server origin
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        // Methods you need
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        // Headers your frontend will send
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        // Headers you want the browser to be able to read
        cfg.setExposedHeaders(List.of("Authorization"));
        // If you do NOT rely on cookies, set this to false and also set axios withCredentials=false
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                // ✅ Never authenticate preflight
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

                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

                        User user = userOpt.get();
                        if (!user.isEnabled()) throw new RuntimeException("User account is disabled");

                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                            email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
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