package com.blog.entity.reaction;

import com.blog.entity.blog.Post;
import com.blog.entity.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@IdClass(PostReactionId.class)
@Table(name = "post_reactions", indexes = {
        @Index(name = "ix_pr_user", columnList = "user_id")
})
public class PostReaction {
    @Id
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Id
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reaction_type_id", nullable = false)
    private ReactionType reactionType;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
}
