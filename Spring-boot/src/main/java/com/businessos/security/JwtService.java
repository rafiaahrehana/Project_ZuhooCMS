package com.businessos.security;


import com.businessos.auth.token.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${jwt.impersonation-expiration-ms:1800000}")
    private long impersonationExpirationMs;


    public String generateAccessToken(String email, String role, Long companyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (companyId != null) {
            claims.put("companyId", companyId);
        }
        return buildToken(claims, email, accessExpirationMs);
    }

    public String generateRefreshToken(String email) {
        return buildToken(new HashMap<>(), email, refreshExpirationMs);
    }

    /**
     * A normal access token with a caller-chosen lifetime. Exists for the
     * public demo session, which must expire on its own schedule (shorter than
     * a login, no refresh) without touching the app-wide access expiry.
     */
    public String generateAccessToken(String email, String role, Long companyId, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (companyId != null) {
            claims.put("companyId", companyId);
        }
        return buildToken(claims, email, expirationMs);
    }

    public String generateActionToken(String email, TokenType actionType, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("actionType", actionType.name());
        return buildToken(claims, email, expirationMs);
    }

    /**
     * Short-lived token for a platform admin/support agent "accessing" a tenant company.
     * Carries the impersonated company's id under the normal "companyId" claim (so every
     * existing tenant-scoping code path picks it up with zero changes) plus impersonatedBy /
     * impersonationSessionId so the JWT filter can flag every request made under it.
     */
    public String generateImpersonationToken(String email, String role, Long companyId,
                                              Long impersonatedBy, String impersonationSessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        if (companyId != null) {
            claims.put("companyId", companyId);
        }
        claims.put("impersonatedBy", impersonatedBy);
        claims.put("impersonationSessionId", impersonationSessionId);
        return buildToken(claims, email, impersonationExpirationMs);
    }

    public long getImpersonationExpirationMs() {
        return impersonationExpirationMs;
    }


    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Long extractCompanyId(String token) {
        Object raw = extractClaims(token).get("companyId");
        if (raw == null) return null;
        if (raw instanceof Long l) return l;
        if (raw instanceof Integer i) return i.longValue();
        return Long.parseLong(raw.toString());
    }

    public TokenType extractActionType(String token) {
        String type = extractClaims(token).get("actionType", String.class);
        return type != null ? TokenType.valueOf(type) : null;
    }

    public Long extractImpersonatedBy(String token) {
        Object raw = extractClaims(token).get("impersonatedBy");
        if (raw == null) return null;
        if (raw instanceof Long l) return l;
        if (raw instanceof Integer i) return i.longValue();
        return Long.parseLong(raw.toString());
    }

    public String extractImpersonationSessionId(String token) {
        return extractClaims(token).get("impersonationSessionId", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
