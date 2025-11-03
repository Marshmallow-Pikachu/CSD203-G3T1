package com.ratewise.security.oauth2;

import com.ratewise.security.entities.Role;
import com.ratewise.security.entities.User;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.repositories.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user info from OAuth2 provider (Google)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Get provider name (google)
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Process and save/update user in database
        processOAuth2User(provider, oAuth2User);

        return oAuth2User;
    }

    private User processOAuth2User(String provider, OAuth2User oAuth2User) {
        // Extract user info from OAuth2User based on provider
        String email = oAuth2User.getAttribute("email");
        String name = extractName(oAuth2User, provider);
        String providerId = extractProviderId(oAuth2User, provider);

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException(
                "Email not found from OAuth2 provider. Please ensure your email is available."
            );
        }

        // Check if user already exists by email
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            // User already exists - no need to create new user
            user = userOptional.get();

            // Update OAuth provider info if it's null (first time OAuth login for existing user)
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(provider);
                user.setOauthProviderId(providerId);
                user = userRepository.save(user);
            }
        } else {
            // New user - create account (first time logging in with this email)
            String username = extractUsername(oAuth2User, provider, email);

            user = User.builder()
                    .username(username)
                    .email(email)
                    .password(null) // No password_hash for OAuth users (empty)
                    .oauthProvider(provider)
                    .oauthProviderId(providerId)
                    .enabled(true) // is_active = true by default
                    .createdAt(LocalDateTime.now()) // Set created_at to current time
                    .build();

            // Save user - UserRepository.create() will generate UUID
            User savedUser = userRepository.save(user);

            // Auto-assign USER role (same as regular registration)
            roleRepository.assignRoleToUser(savedUser.getId(), Role.ROLE_USER);

            user = savedUser;
        }

        return user;
    }

    /**
     * Extract name based on provider
     */
    private String extractName(OAuth2User oAuth2User, String provider) {
        return oAuth2User.getAttribute("name");
    }

    /**
     * Extract username for database storage based on provider
     */
    private String extractUsername(OAuth2User oAuth2User, String provider, String email) {
        if ("google".equals(provider)) {
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
