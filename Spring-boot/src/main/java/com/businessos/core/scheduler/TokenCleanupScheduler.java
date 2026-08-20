package com.businessos.core.scheduler;

import com.businessos.auth.token.TokenRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Removes expired tokens from the database.
 *
 * Without this scheduler the tokens table grows indefinitely.
 * Every registration + login + password reset creates token rows.
 * Most of them expire after 15 minutes to 24 hours but are never cleaned up.
 *
 * Schedule: nightly at 03:00 — low-traffic window.
 * Retention: removes tokens that expired more than 7 days ago.
 * The 7-day grace period preserves recent tokens for debugging if needed.
 */
@Component
@RequiredArgsConstructor

public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;

    private static final int GRACE_PERIOD_DAYS = 7;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        tokenRepository.deleteExpiredBefore(cutoff);
        
    }
}
