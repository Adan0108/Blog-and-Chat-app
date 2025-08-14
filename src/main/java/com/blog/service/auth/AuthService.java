package com.blog.service.auth;

import com.blog.dto.request.auth.LoginRequest;
import com.blog.dto.request.auth.SignUpRequest;
import com.blog.dto.response.auth.AuthResponse;

public interface AuthService {
    AuthResponse signUp(SignUpRequest req);
    AuthResponse login(LoginRequest req, String userAgent, String ipAddress);
    AuthResponse refresh(Long userId, String refreshToken);
    void logout(Long userId, String accessHeader);
}
