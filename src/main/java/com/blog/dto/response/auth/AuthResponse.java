package com.blog.dto.response.auth;

public record AuthResponse(Long userId, String email, TokenPair tokens) {}
