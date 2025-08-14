package com.blog.entity.blog;

import com.blog.entity.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "posts", indexes = {
        @Index(name = "ix_posts_user", columnList = "user_id")
})
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "maximum_images", nullable = false)
    private Integer maximumImages = 15;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    // getters/setters
}
