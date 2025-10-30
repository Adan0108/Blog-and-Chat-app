package com.blog.controller.social;

import com.blog.repository.social.UserFollowersRepository;
import com.blog.entity.social.RelationType;
import com.blog.entity.social.UserFollower;
import com.blog.entity.user.User;
import com.blog.repository.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final UserRepository users;
    private final UserFollowersRepository followers;

    public SocialController(UserRepository users, UserFollowersRepository followers) {
        this.users = users;
        this.followers = followers;
    }

    private Long currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : Long.valueOf(String.valueOf(a.getPrincipal()));
    }

    @PostMapping("/follow/{targetId}")
    @Transactional
    public ResponseEntity<Void> follow(@PathVariable Long targetId) {
        Long me = currentUserId();
        if (me.equals(targetId)) return ResponseEntity.badRequest().build();
        if (!users.existsById(targetId)) return ResponseEntity.notFound().build();

        followers.upsertRelation(me, targetId, RelationType.FOLLOWER.name(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/friend/{targetId}")
    @Transactional
    public ResponseEntity<Void> befriend(@PathVariable Long targetId) {
        Long me = currentUserId();
        if (me.equals(targetId)) return ResponseEntity.badRequest().build();
        if (!users.existsById(targetId)) return ResponseEntity.notFound().build();

        followers.upsertRelation(me, targetId, RelationType.FRIEND.name(), Instant.now());
        followers.upsertRelation(targetId, me, RelationType.FRIEND.name(), Instant.now()); // mutual
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/unfollow/{targetId}")
    @Transactional
    public ResponseEntity<Void> unfollow(@PathVariable Long targetId) {
        Long me = currentUserId();
        followers.deleteByFollowerIdAndFollowingId(me, targetId);
        return ResponseEntity.noContent().build();
    }
}
