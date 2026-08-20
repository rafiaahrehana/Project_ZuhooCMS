package com.businessos.security;

import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        boolean mdcSet = false;

        try {
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && user.isEnabled()) {
                    Long companyId = jwtService.extractCompanyId(token);
                    Long impersonatedBy = jwtService.extractImpersonatedBy(token);

                    // Impersonation tokens grant access as the impersonated tenant role
                    // (see ImpersonationServiceImpl), NOT the real admin's own DB role -
                    // otherwise every tenant endpoint would 403 a platform admin/support
                    // agent trying to "access" a company.
                    Collection<? extends GrantedAuthority> authorities;
                    if (impersonatedBy != null) {
                        String impersonatedRole = jwtService.extractRole(token);
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + impersonatedRole));

                        MDC.put("impersonatedBy", String.valueOf(impersonatedBy));
                        String sessionId = jwtService.extractImpersonationSessionId(token);
                        if (sessionId != null) {
                            MDC.put("impersonationSessionId", sessionId);
                        }
                        mdcSet = true;
                    } else {
                        authorities = user.getAuthorities();
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, companyId, authorities);

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Never let a malformed/unexpected token silently pass through as
            // authenticated - log it so a real bug here (vs. just a bad token) doesn't
            // go unnoticed. Deliberately not logging the token itself.
            log.warn("JWT authentication failed: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (mdcSet) {
                MDC.remove("impersonatedBy");
                MDC.remove("impersonationSessionId");
            }
        }
    }
}
