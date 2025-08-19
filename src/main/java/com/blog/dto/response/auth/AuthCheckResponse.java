package com.blog.dto.response.auth;

public record AuthCheckResponse(boolean loggedIn, Long userId, String email, String username) {}
