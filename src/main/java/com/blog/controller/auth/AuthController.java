package com.blog.controller.auth;

import com.blog.config.security.CookieUtil;
import com.blog.dto.request.auth.LoginRequest;
import com.blog.dto.request.auth.SignUpRequest;
import com.blog.dto.response.auth.AuthCheckResponse;
import com.blog.dto.response.auth.AuthResponse;
import com.blog.dto.response.auth.TokenPair;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;
    private final UserProfileRepository profiles;

    public AuthController(AuthService authService,
                          UserRepository users,
                          UserProfileRepository profiles) {
        this.authService = authService;
        this.users = users;
        this.profiles = profiles;
    }

    private boolean isProd() {
        String p = System.getenv("APP_ENV");
        return p != null && p.equalsIgnoreCase("production");
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest req) {
        AuthResponse resp = authService.signUp(req);
        return ResponseEntity.status(201).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                              HttpServletRequest http) {
        String ip = Optional.ofNullable(http.getHeader("X-Forwarded-For")).orElse(http.getRemoteAddr());
        AuthResponse resp = authService.login(req, userAgent, ip);

        // Set RT cookie (HttpOnly).
        if (resp.tokens() != null && resp.tokens().refreshToken() != null) {
            ResponseCookie rtCookie = CookieUtil.buildRefreshCookie(
                    resp.tokens().refreshToken(),
                    com.blog.config.security.JwtUtil.REFRESH_TTL,
                    isProd()
            );

            // *** Do NOT return RT in body — only AT ***
            TokenPair bodyPair = new TokenPair(resp.tokens().accessToken(), null); // null RT
            AuthResponse body = new AuthResponse(resp.userId(), resp.email(), bodyPair);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                    .body(body);
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("x-client-id") Long userId,
                                                @CookieValue(name = CookieUtil.RT_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        AuthResponse resp = authService.refresh(userId, refreshToken);

        // Rotate RT cookie
        if (resp.tokens() != null && resp.tokens().refreshToken() != null) {
            ResponseCookie rtCookie = CookieUtil.buildRefreshCookie(
                    resp.tokens().refreshToken(),
                    com.blog.config.security.JwtUtil.REFRESH_TTL,
                    isProd()
            );

            // *** Do NOT return RT in body — only AT ***
            TokenPair bodyPair = new TokenPair(resp.tokens().accessToken(), null); // null RT
            AuthResponse body = new AuthResponse(resp.userId(), resp.email(), bodyPair);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, rtCookie.toString())
                    .body(body);
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("x-client-id") Long userId,
                                       @RequestHeader("authorization") String accessHeader) {
        authService.logout(userId, accessHeader);
        var clear = CookieUtil.clearRefreshCookie(isProd());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clear.toString())
                .build();
    }

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> check() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.valueOf(String.valueOf(authentication.getPrincipal()));
        var u = users.findById(userId).orElseThrow();
        var p = profiles.findByUserId(userId).orElse(null);
        String username = p != null ? p.getUsername() : null;

        return ResponseEntity.ok(new AuthCheckResponse(true, userId, u.getEmail(), username));
    }
}