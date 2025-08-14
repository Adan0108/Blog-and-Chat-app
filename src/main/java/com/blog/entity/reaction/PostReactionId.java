package com.blog.entity.reaction;

import java.io.Serializable;
import java.util.Objects;

public class PostReactionId implements Serializable {
    private Long post;
    private Long user;

    public PostReactionId() {}
    public PostReactionId(Long post, Long user){ this.post = post; this.user = user; }

    @Override public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof PostReactionId)) return false;
        var that = (PostReactionId)o;
        return Objects.equals(post, that.post) && Objects.equals(user, that.user);
    }
    @Override public int hashCode(){ return Objects.hash(post, user); }
}
