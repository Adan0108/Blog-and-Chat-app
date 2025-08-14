package com.blog.config.security;

import com.blog.dto.response.auth.TokenPair;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class JwtUtil {
    private JwtUtil() {}

    public static final Duration ACCESS_TTL  = Duration.ofDays(2);
    public static final Duration REFRESH_TTL = Duration.ofDays(7);

    private static Key hmac(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static TokenPair createTokenPair(Long userId, String email,
                                            String publicKeyForAT, String privateKeyForRT,
                                            String jti) {
        Instant now = Instant.now();

        String at = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti)
                .claim("uid", userId)
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ACCESS_TTL)))
                .signWith(hmac(publicKeyForAT), SignatureAlgorithm.HS256)
                .compact();

        String rt = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti)
                .claim("uid", userId)
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(REFRESH_TTL)))
                .signWith(hmac(privateKeyForRT), SignatureAlgorithm.HS256)
                .compact();

        return new TokenPair(at, rt);
    }

    public static Jws<Claims> verifyAccess(String token, String publicKeyForAT) {
        return Jwts.parserBuilder().setSigningKey(hmac(publicKeyForAT)).build().parseClaimsJws(token);
    }

    public static Jws<Claims> verifyRefresh(String token, String privateKeyForRT) {
        return Jwts.parserBuilder().setSigningKey(hmac(privateKeyForRT)).build().parseClaimsJws(token);
    }
}
