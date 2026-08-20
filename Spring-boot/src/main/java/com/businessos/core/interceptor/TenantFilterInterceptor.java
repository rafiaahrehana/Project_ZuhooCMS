package com.businessos.core.interceptor;

import com.businessos.security.SecurityUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.businessos.auth.user.User;

@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;
    private final SecurityUtil securityUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Long companyId = securityUtil.getCurrentCompanyId();
        User user = securityUtil.getCurrentUser();
        // Only restrict standard tenant users. Platform staff bypasses the automatic filter.
        if (companyId != null && user != null && user.isTenantUser()) {
            entityManager
                    .unwrap(Session.class)
                    .enableFilter("tenantFilter")
                    .setParameter("companyId", companyId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") != null) {
                session.disableFilter("tenantFilter");
            }
        } catch (Exception ignored) {
            // Session may already be closed after request completes — safe to ignore
        }
    }
}
