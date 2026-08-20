package com.businessos.auth.token;

/**
 * Token types for different authentication purposes
 */
public enum TokenType {
    /**
     * Access token for API authentication
     */
    ACCESS,

    /**
     * Refresh token for obtaining new access tokens
     */
    REFRESH,

    /**
     * Email capture token
     */
    EMAIL_VERIFICATION,

    /**
     * Password reset token
     */
    PASSWORD_RESET
}
