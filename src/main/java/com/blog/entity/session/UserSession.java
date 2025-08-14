package com.blog.entity.session;

import com.blog.entity.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_sessions", indexes = {
        @Index(name = "ix_us_user", columnList = "user_id"),
        @Index(name = "ix_us_expires", columnList = "expires_at"),
        @Index(name = "uq_us_jti", columnList = "jti", unique = true)
})
public class UserSession {
    @Id @Column(name = "session_id", length = 36)
    private String sessionId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 36, nullable = false)
    private String jti;

    @Column(length = 45)
    private String ipAddress;

    @Lob
    private String userAgent;

    @Column(name = "is_revoked", nullable = false)
    private Boolean revoked = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant lastActive = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    // getters/setters
}

