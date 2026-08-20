package com.businessos.modules.demo;

import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * The one choke point that makes the public demo safe: any request
 * authenticated as the demo account may read but never write.
 *
 * This is deliberately a method filter rather than a read-only permission set.
 * The permission system has ~250 codes, COMPANY_OWNER bypasses it entirely
 * (which the demo user relies on to SEE every module), and one missed check
 * would mean an anonymous visitor writing into the database. GET-or-403 cannot
 * be bypassed by a UI gap or a forgotten endpoint.
 *
 * Runs after JwtAuthFilter (it needs the authenticated user) - see
 * SecurityConfig for the ordering.
 */
@Component
@RequiredArgsConstructor
public class DemoReadOnlyFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;
    // Constructed, not injected: this application exposes no ObjectMapper bean
    // (SubscriptionEnforcementFilter does the same).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        User user = securityUtil.getCurrentUser();
        if (user == null || !DemoDataSeeder.DEMO_OWNER_EMAIL.equalsIgnoreCase(user.getEmail())) {
            chain.doFilter(request, response);
            return;
        }

        // Logout is the one mutation a demo visitor legitimately performs.
        if (request.getRequestURI().equals("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        // Some list endpoints are POSTs only because their filter criteria
        // travel in a request body (e.g. /api/crm/leads/filter). They read,
        // they don't write - blocking them blanks the page in demo mode.
        String uri = request.getRequestURI();
        if ("POST".equals(method) && (uri.endsWith("/filter") || uri.endsWith("/search"))) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "message", "This is a read-only demo - sign up free to try it with your own data.",
                "demo", true)));
    }
}
