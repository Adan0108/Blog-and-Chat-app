package com.blog.entity.session;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "used_refresh_tokens", indexes = {
        @Index(name = "ix_urt_session", columnList = "session_id")
})
public class UsedRefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id", nullable = false)
    private UserSession session;

    @Column(length = 36, nullable = false)
    private String jti;

    @Column(nullable = false)
    private Instant usedAt = Instant.now();

    // getters/setters
}

