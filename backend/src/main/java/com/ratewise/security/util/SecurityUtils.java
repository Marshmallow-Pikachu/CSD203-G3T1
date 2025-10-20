package com.ratewise.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    
    /**
     * Get the current authenticated user's email from SecurityContext
     * @return email of authenticated user, or null if not authenticated
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !authentication.getPrincipal().equals("anonymousUser")) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * Check if current user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
               && !authentication.getPrincipal().equals("anonymousUser");
    }
    
    /**
     * Get current Authentication object
     * @return current Authentication or null if not authenticated
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}