package com.blog.controller.auth;

import com.blog.dto.request.auth.LoginRequest;
import com.blog.dto.request.auth.SignUpRequest;
import com.blog.dto.response.auth.AuthResponse;
import com.blog.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest req) {
        return ResponseEntity.status(201).body(auth.signUp(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
                                              @RequestHeader(value="User-Agent", required=false) String userAgent,
                                              HttpServletRequest http) {
        String ip = Optional.ofNullable(http.getHeader("X-Forwarded-For")).orElse(http.getRemoteAddr());
        return ResponseEntity.ok(auth.login(req, userAgent, ip));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("x-client-id") Long userId,
                                                @RequestHeader("x-rtoken-id") String refreshToken) {
        return ResponseEntity.ok(auth.refresh(userId, refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("x-client-id") Long userId,
                                       @RequestHeader("authorization") String accessHeader) {
        auth.logout(userId, accessHeader);
        return ResponseEntity.noContent().build();
    }
}
