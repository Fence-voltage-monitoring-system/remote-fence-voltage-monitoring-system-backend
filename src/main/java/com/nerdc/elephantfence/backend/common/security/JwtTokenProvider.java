package com.nerdc.elephantfence.backend.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationInMs;
    private final long refreshExpirationInMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:RemoteElephantFenceMonitoringSystemSecretKey2026SecureKey123!}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationInMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationInMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.refreshExpirationInMs = refreshExpirationInMs;
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String sessionId = userPrincipal.getSessionId() != null ? userPrincipal.getSessionId() : UUID.randomUUID().toString();
        return generateToken(authentication, sessionId);
    }

    public String generateToken(Authentication authentication, String sessionId) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateTokenFromUserId(userPrincipal.getId(), jwtExpirationInMs, sessionId);
    }

    public String generateRefreshToken(UUID userId) {
        return generateRefreshToken(userId, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(UUID userId, String sessionId) {
        return generateTokenFromUserId(userId, refreshExpirationInMs, sessionId);
    }

    public String generateTokenFromUserId(UUID userId, long expirationMs) {
        return generateTokenFromUserId(userId, expirationMs, UUID.randomUUID().toString());
    }

    public String generateTokenFromUserId(UUID userId, long expirationMs, String sessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public UUID getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public String getSessionIdFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("sessionId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
