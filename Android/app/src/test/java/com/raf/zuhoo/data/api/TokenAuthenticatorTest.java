package com.raf.zuhoo.data.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rule that decides whether a 401 means "token expired" (refresh and retry) or "wrong
 * credentials" (don't).
 *
 * Getting this backwards on the login path would clear the session of anyone who mistypes their
 * password — a bug that only ever shows up in front of a real user.
 */
public class TokenAuthenticatorTest {

    @Test
    public void loginIsNeverRefreshed() {
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/login"));
    }

    @Test
    public void refreshItselfIsNeverRefreshed() {
        // Otherwise a rejected refresh token re-enters the authenticator and recurses.
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/refresh"));
    }

    @Test
    public void publicRegistrationPathsAreNeverRefreshed() {
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/register"));
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/clients/public/register"));
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/forgot-password"));
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/reset-password"));
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/verify-email"));
        assertTrue(TokenAuthenticator.isUnauthenticatedPath("/api/auth/resend-verification"));
    }

    @Test
    public void ordinaryApiCallsAreRefreshed() {
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/service-requests/my"));
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/company/finance/invoices/me"));
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/notifications/count"));
    }

    @Test
    public void authenticatedAuthEndpointsAreStillRefreshed() {
        // change-password and logout live under /api/auth but DO require a token, so an expired
        // one there should be refreshed like anywhere else.
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/auth/change-password"));
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/auth/logout"));
    }

    @Test
    public void matchIsAnchoredToTheEndOfThePath() {
        // A path that merely *contains* a skip token must not be skipped — otherwise a future
        // endpoint like /api/audit/api/auth/login-attempts would silently stop refreshing.
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/auth/login-attempts"));
        assertFalse(TokenAuthenticator.isUnauthenticatedPath("/api/auth/login/history"));
    }

    @Test
    public void nullPathIsTreatedAsRefreshable() {
        assertFalse(TokenAuthenticator.isUnauthenticatedPath(null));
    }
}
