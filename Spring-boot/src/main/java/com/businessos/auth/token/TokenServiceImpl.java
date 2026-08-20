package com.businessos.auth.token;

import com.businessos.auth.user.User;
import com.businessos.security.JwtService;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    /**
     * Create and persist a refresh token for a platformuser
     */
    @Override
    public String createRefreshToken(User user) {
        revokeAllRefreshTokens(user);

        UserToken token = UserToken.builder()
                .token(generateTokenValue())
                .user(user)
                .tokenType(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();

        tokenRepository.save(token);
        return token.getToken();
    }

    /**
     * Validate a refresh token and return associated platformuser if valid
     */
    @Override
    public User validateRefreshToken(String tokenValue) {
        return tokenRepository.findByTokenAndType(tokenValue, TokenType.REFRESH)
                .filter(UserToken::isValid)
                .map(UserToken::getUser)
                .orElseThrow(() -> new BadRequestException("Invalid or expired refresh token"));
    }

    /**
     * Revoke a specific token
     */
    @Override
    public void revokeRefreshToken(String tokenValue) {
        tokenRepository.findByTokenAndType(tokenValue, TokenType.REFRESH)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    tokenRepository.save(token);
                });
    }

    /**
     * Revoke all refresh tokens for a platformuser (typically on logout)
     */
    @Override
    public void revokeAllRefreshTokens(User user) {
        tokenRepository.revokeAllByUserIdAndType(user.getId(), TokenType.REFRESH);
    }

    /**
     * Clean up expired tokens (typically run as scheduled task)
     */
    @Override
    public void deleteExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
    }

    /**
     * Generate a unique token value
     */
    private String generateTokenValue() {
        return java.util.UUID.randomUUID().toString();
    }

}
