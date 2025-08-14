package com.blog.entity.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "user_keys")
public class UserKeyPair {
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @Lob @Column(nullable = false)
    private String publicKey;

    @Lob @Column(nullable = false)
    private String privateKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    // getters/setters
}
