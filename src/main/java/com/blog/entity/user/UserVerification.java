package com.blog.entity.user;


import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "user_verifications", indexes = {
        @Index(name = "ix_uv_user_verified_expiry", columnList = "user_id,is_verified,expires_at")
})
public class UserVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String otpCode;

    @Column(length = 255)
    private String otpHash;

    @Column(nullable = false, length = 20)
    private String type; // e.g. EMAIL, MOBILE

    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // getters/setters
}
