package com.blog.entity.comment;

import com.blog.entity.blog.Post;
import com.blog.entity.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "user_comments", indexes = {
        @Index(name = "ix_uc_post", columnList = "post_id"),
        @Index(name = "ix_uc_user", columnList = "user_id"),
        @Index(name = "ix_uc_parent", columnList = "parent_id")
})
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private Comment parent;

    @Lob @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // getters/setters
}

