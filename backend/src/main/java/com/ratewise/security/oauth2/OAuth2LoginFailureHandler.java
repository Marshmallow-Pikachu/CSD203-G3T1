package com.ratewise.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = exception.getMessage();
        String errorType = "oauth_failed";

        if (errorMessage != null && errorMessage.contains("Email not found")) {
            errorType = "email_required";
        }

        String targetUrl = "http://localhost:5173/login?error=" + errorType + "&message=" +
                          java.net.URLEncoder.encode(errorMessage, "UTF-8");
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
