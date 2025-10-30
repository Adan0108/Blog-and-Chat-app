package com.blog.entity.reaction;

import com.blog.entity.blog.Post;
import com.blog.entity.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "post_reactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_user", columnNames = {"post_id","user_id"}))
public class PostReaction {

    @EmbeddedId
    private PostReactionId id = new PostReactionId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private ReactionType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // make nullable for first migration to avoid 0000-00-00, Hibernate will keep it updated
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    // getters/setters
    public PostReactionId getId() { return id; }
    public void setId(PostReactionId id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ReactionType getType() { return type; }
    public void setType(ReactionType type) { this.type = type; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
