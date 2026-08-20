package com.businessos.security;

import com.businessos.auth.user.User;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides convenient access to the currently authenticated platformuser and
 * their resolved company ID (stored as authentication details by the
 * JWT filter after token validation).
 */
@Component
public class SecurityUtil {

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    public Long getCurrentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;
        Object credentials = auth.getCredentials();
        return (credentials instanceof Long id) ? id : null;
    }

    public boolean isImpersonating() {
        return MDC.get("impersonatedBy") != null;
    }

    public Long getImpersonatedByUserId() {
        String raw = MDC.get("impersonatedBy");
        return raw != null ? Long.parseLong(raw) : null;
    }
}
