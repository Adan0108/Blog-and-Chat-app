package com.blog.entity.social;

import java.io.Serializable;
import java.util.Objects;

public class UserFollowerId implements Serializable {
    // names MUST match the @Id property names in the entity
    private Long follower;
    private Long following;

    public UserFollowerId() {}
    public UserFollowerId(Long follower, Long following) {
        this.follower = follower;
        this.following = following;
    }

    // (optional) getters/setters if you prefer
    public Long getFollower() { return follower; }
    public void setFollower(Long follower) { this.follower = follower; }
    public Long getFollowing() { return following; }
    public void setFollowing(Long following) { this.following = following; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserFollowerId that)) return false;
        return Objects.equals(follower, that.follower)
                && Objects.equals(following, that.following);
    }
    @Override public int hashCode() { return Objects.hash(follower, following); }
}
