package com.businessos.auth.token;

import com.businessos.auth.user.User;

public interface TokenService {

    String createRefreshToken(User user);

    User validateRefreshToken(String token);

    void revokeRefreshToken(String token);

    void revokeAllRefreshTokens(User user);

    void deleteExpiredTokens();
}

