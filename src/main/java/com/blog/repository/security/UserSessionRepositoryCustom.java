package com.blog.repository.security;

import com.blog.entity.session.UserSession;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepositoryCustom {
    Optional<UserSession> findActive(Long userId, String jti, Instant now);
    int revoke(Long userId, String jti);
}
