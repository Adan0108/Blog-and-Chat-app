package com.blog.controller.auth;

import com.blog.dto.request.auth.LoginRequest;
import com.blog.dto.request.auth.SignUpRequest;
import com.blog.dto.response.auth.AuthCheckResponse;
import com.blog.dto.response.auth.AuthResponse;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest req) {
        return ResponseEntity.status(201).body(authService.signUp(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                              HttpServletRequest http) {
        String ip = Optional.ofNullable(http.getHeader("X-Forwarded-For")).orElse(http.getRemoteAddr());
        return ResponseEntity.ok(authService.login(req, userAgent, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("x-client-id") Long userId,
                                                @RequestHeader("x-rtoken-id") String refreshToken) {
        return ResponseEntity.ok(authService.refresh(userId, refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("x-client-id") Long userId,
                                       @RequestHeader("authorization") String accessHeader) {
        authService.logout(userId, accessHeader);
        return ResponseEntity.noContent().build();
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