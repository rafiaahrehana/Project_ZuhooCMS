package com.businessos.security;

import com.businessos.auth.user.User;
import com.businessos.enums.CompanyStatus;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SubscriptionEnforcementFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionEnforcementFilter(SecurityUtil securityUtil, 
                                         CompanyRepository companyRepository, 
                                         @Lazy ObjectMapper objectMapper) {
        this.securityUtil = securityUtil;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 1. Skip safe methods
        if (HttpMethod.GET.matches(method) || 
            HttpMethod.OPTIONS.matches(method) || 
            HttpMethod.HEAD.matches(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Skip public endpoints (they shouldn't require subscription checks anyway)
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/companies/public/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract platformuser & company
        User user = securityUtil.getCurrentUser();
        Long companyId = securityUtil.getCurrentCompanyId();

        if (user != null && companyId != null) {
            Company company = companyRepository.findById(companyId).orElse(null);

            // Previously this only checked subscriptionEnd date - an admin
            // suspending a tenant for a ToS/fraud hold (Company.status =
            // SUSPENDED/DEACTIVATED) had zero effect here: every write endpoint
            // kept working until their unrelated billing date happened to expire.
            boolean adminSuspended = company != null
                    && (company.getStatus() == CompanyStatus.SUSPENDED
                        || company.getStatus() == CompanyStatus.DEACTIVATED);

            if (company != null && (adminSuspended || company.isTrialExpired())) {
                // 4. Check exceptions for expired companies. An admin-suspended
                // company gets none of them - those exist so a company can pay
                // its way out of a *billing* lapse; a suspension for cause
                // shouldn't be escapable by hitting the payment endpoint.
                boolean isAllowedEndpoint = !adminSuspended && (
                                            uri.matches("^/api/support/tickets.*") ||
                                            uri.matches("^/api/invoices.*") ||
                                            uri.matches("^/api/payment.*") ||
                                            uri.matches("^/api/wallet.*"));

                if (!isAllowedEndpoint) {
                    // Block the request
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");

                    Map<String, String> error = new HashMap<>();
                    error.put("error", adminSuspended ? "Account Suspended" : "Subscription Expired");
                    error.put("message", adminSuspended
                            ? "Your account has been suspended. Please contact support."
                            : "Your subscription or trial has expired. The system is in read-only mode. Please process payment or contact support to continue.");

                    response.getWriter().write(objectMapper.writeValueAsString(error));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
