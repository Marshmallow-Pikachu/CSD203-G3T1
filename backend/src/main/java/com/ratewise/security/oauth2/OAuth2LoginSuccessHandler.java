package com.ratewise.security.oauth2;

import com.ratewise.security.entities.Role;
import com.ratewise.security.entities.User;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Data
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OAuth2LoginSuccessHandler(JWTUtil jwtUtil, UserRepository userRepository, RoleRepository roleRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Get OAuth2User from authentication
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Determine OAuth provider and extract user info accordingly
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String email = extractEmail(oAuth2User, provider);
        String name = extractName(oAuth2User, provider);
        String providerId = extractProviderId(oAuth2User, provider);

        if (email == null) {
            throw new RuntimeException("Email not found from OAuth2 provider");
        }

        // Find or create user in database
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isEmpty()) {
            // User doesn't exist - create new user
            String username = extractUsername(oAuth2User, provider, email);

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(null) // No password for OAuth users
                    .oauthProvider(provider)
                    .oauthProviderId(providerId)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save user - UserRepository.create() will generate UUID
            User savedUser = userRepository.save(user);

            // Assign USER role
            roleRepository.assignRoleToUser(savedUser.getId(), Role.ROLE_USER);

            user = savedUser;
        } else {
            user = userOptional.get();
        }

        // Load user with role for JWT generation
        User userWithRole = userRepository.findByIdWithRole(user.getId())
                .orElseThrow(() -> new RuntimeException("User role not found"));

        // Generate YOUR JWT token (same as regular login)
        String jwtToken = jwtUtil.generateToken(userWithRole);

        // Build redirect URL to frontend with JWT token as query parameter
        String frontendBase = resolveFrontendBaseUrl(request);
        String targetUrl = frontendBase + "/oauth-callback#token=" + URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);

        // Redirect to frontend with JWT token
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Determine which OAuth provider was used based on available attributes
     */
    private String determineProvider(OAuth2User oAuth2User) {
        // Google uses "sub" attribute
        if (oAuth2User.getAttribute("sub") != null) {
            return "google";
        }
        throw new RuntimeException("Unknown OAuth provider");
    }

    /**
     * Extract email based on provider
     */
    private String extractEmail(OAuth2User oAuth2User, String provider) {
        return oAuth2User.getAttribute("email");
    }

    /**
     * Extract name based on provider (used for display purposes)
     */
    private String extractName(OAuth2User oAuth2User, String provider) {
        if ("google".equals(provider)) {
            return oAuth2User.getAttribute("name");
        }
        return null;
    }


    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        // 1) Environment variable override (configure in Azure → Configuration)
        String configured = System.getenv("FRONTEND_BASE_URL");
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", ""); // trim trailing slash(es)
        }

        // 2) Reverse-proxy aware (Azure App Service / App Gateway / Nginx)
        String proto = Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
                            .orElse(request.getScheme());
        String hostHeader = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                                    .orElse(request.getHeader("Host"));
        if (hostHeader != null && !hostHeader.isBlank()) {
            return proto + "://" + hostHeader;
        }

        // 3) Fallback to serverName:port
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = (("http".equalsIgnoreCase(proto) && port == 80) ||
                                ("https".equalsIgnoreCase(proto) && port == 443));
        return proto + "://" + host + (isDefaultPort ? "" : ":" + port);
    }

    /**
     * Extract username for database storage based on provider
     */
    private String extractUsername(OAuth2User oAuth2User, String provider, String email) {
        if ("google".equals(provider)) {
            // Google: use name if available, otherwise derive from email
            String name = oAuth2User.getAttribute("name");
            return name != null && !name.isEmpty() ? name : email.split("@")[0];
        }
        return email.split("@")[0];
    }

    /**
     * Extract provider-specific user ID
     */
    private String extractProviderId(OAuth2User oAuth2User, String provider) {
        if ("google".equals(provider)) {
            return oAuth2User.getAttribute("sub");
        }
        return null;
    }
    

}
