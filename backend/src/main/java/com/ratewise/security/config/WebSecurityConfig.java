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
import org.springframework.security.config.Customizer;
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
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.entities.User;
import com.ratewise.security.oauth2.CustomOAuth2UserService;
import com.ratewise.security.oauth2.OAuth2LoginSuccessHandler;
import com.ratewise.security.oauth2.OAuth2LoginFailureHandler;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    public WebSecurityConfig(JWTUtil jwtUtil,
                           UserRepository userRepository,
                           CustomOAuth2UserService customOAuth2UserService,
                           OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                           OAuth2LoginFailureHandler oAuth2LoginFailureHandler) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
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
                        // OAuth2 endpoints
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Public authentication endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/registration").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/session").permitAll()
                        // Protected authentication endpoints (any authenticated user)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/session").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/profile").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/validation").hasAnyRole("ADMIN", "USER")

                        .requestMatchers("/api/v1/health/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/db/**").hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/calculator/landed-cost").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/tariffs/**").hasAnyRole("ADMIN", "USER")

                        // Admin-only endpoints
                        .requestMatchers("/api/v1/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "api/v1/admin/roles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/v1/admin/tariffs/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/v1/admin/tariffs/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/admin/tariffs/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/v1/admin/tariffs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/v1/admin/tariffs").hasRole("ADMIN")
                        .requestMatchers("/api/v1/countries/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/v1/agreements/**").hasRole("ADMIN")

                        // Require authentication for any other request
                        .anyRequest().hasRole("ADMIN"))
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService))
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler))
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

                        // Check if token is still active (not logged out)
                        if (!jwtUtil.isTokenActiveForUser(token)) {
                            throw new RuntimeException("Token has been invalidated");
                        }

                        String userId = jwtUtil.getUserId(token);
                        String email = jwtUtil.getEmail(token);
                        String role = jwtUtil.getRole(token);

                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

                        User user = userOpt.get();
                        if (!user.isEnabled()) throw new RuntimeException("User account is disabled");

                        // Convert role to Spring Security authority
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        );

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
                // Only run JWT auth for API paths; skip everything else (static, SPA, swagger, etc.)
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

    // CORS configuration (might not even be needed since we are using same app service to run both frontend and back end so its the same origin
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
            "http://localhost:5173"
            // Add more only if you host the frontend separately
            // "https://<your-frontend>.azurestaticapps.net"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
