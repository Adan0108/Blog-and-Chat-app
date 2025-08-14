package com.blog.entity.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "user_profiles", indexes = {
        @Index(name = "ix_user_profiles_username", columnList = "username", unique = true),
        @Index(name = "ix_user_profiles_mobile", columnList = "mobile", unique = true)
})
public class UserProfile {
    @Id
    private Long userId; // PK=FK

    @MapsId
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(length = 100)
    private String nickname;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    private Short state = 1;

    @Column(length = 20, unique = true)
    private String mobile;

    private Short gender; // 0/1/2…

    private LocalDate birthday;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Lob
    private String bioDescription;

    private Instant deletedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // getters/setters
}
