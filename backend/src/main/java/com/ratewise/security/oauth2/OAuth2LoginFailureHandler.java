package com.ratewise.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String rawMessage = (exception != null && exception.getMessage() != null)
                ? exception.getMessage()
                : "Authentication failed";

        // Map to a lightweight error code your SPA can switch on
        String errorType = rawMessage.contains("Email not found") ? "email_required" : "oauth_failed";

        // (Optional) keep message short & safe for UI (avoid stack traces / internals)
        String safeMessage = rawMessage.length() > 300 ? rawMessage.substring(0, 300) + "..." : rawMessage;

        String frontendBase = resolveFrontendBaseUrl(request);

        // Use a fragment to reduce leakage via Referer headers:
        // e.g. https://app/login#error=oauth_failed&message=...
        String targetUrl = frontendBase
                + "/login#error=" + URLEncoder.encode(errorType, StandardCharsets.UTF_8)
                + "&message=" + URLEncoder.encode(safeMessage, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);


        
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


}
