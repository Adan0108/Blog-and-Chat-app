package com.blog.config.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {
    private CookieUtil() {}

    public static final String RT_COOKIE = "rt"; // cookie name

    public static ResponseCookie buildRefreshCookie(String refreshToken, Duration ttl, boolean production) {
        return ResponseCookie.from(RT_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(production)     // set true in prod
                .sameSite("Strict")
                .path("/")
                .maxAge(ttl)
                .build();
    }

    public static ResponseCookie clearRefreshCookie(boolean production) {
        return ResponseCookie.from(RT_COOKIE, "")
                .httpOnly(true)
                .secure(production)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
