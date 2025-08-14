package com.blog.entity.social;

import com.blog.entity.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@IdClass(UserFollowerId.class)
@Table(name = "user_followers", indexes = {
        @Index(name = "ix_following", columnList = "following_id")
})
public class UserFollower {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RelationType relationType = RelationType.FOLLOWER;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }
    public User getFollowing() { return following; }
    public void setFollowing(User following) { this.following = following; }
    public RelationType getRelationType() { return relationType; }
    public void setRelationType(RelationType relationType) { this.relationType = relationType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
