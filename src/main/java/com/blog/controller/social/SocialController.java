package com.blog.controller.social;

import com.blog.dto.request.social.PrivacyRequest;
import com.blog.dto.response.social.FollowRequestView;
import com.blog.service.social.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService social;

    public SocialController(SocialService social) {
        this.social = social;
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        return Long.valueOf(String.valueOf(auth.getPrincipal()));
    }

    // ---- FOLLOW / UNFOLLOW / REMOVE FOLLOWER ----
    @PostMapping("/follow/{targetId}")
    @Transactional
    public ResponseEntity<?> follow(@PathVariable Long targetId) {
        social.follow(currentUserId(), targetId); // immediate or request (if private)
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/unfollow/{targetId}")
    @Transactional
    public ResponseEntity<?> unfollow(@PathVariable Long targetId) {
        social.unfollow(currentUserId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/followers/{followerId}")
    @Transactional
    public ResponseEntity<?> removeFollower(@PathVariable Long followerId) {
        social.removeFollower(currentUserId(), followerId);
        return ResponseEntity.noContent().build();
    }

    // ---- FOLLOW REQUESTS ----
    @GetMapping("/follow-requests/incoming")
    public ResponseEntity<List<FollowRequestView>> incoming() {
        return ResponseEntity.ok(social.getIncomingFollowRequests(currentUserId()));
    }

    @GetMapping("/follow-requests/outgoing")
    public ResponseEntity<List<FollowRequestView>> outgoing() {
        return ResponseEntity.ok(social.getOutgoingFollowRequests(currentUserId()));
    }

    @PostMapping("/follow-requests/{requestId}/accept")
    @Transactional
    public ResponseEntity<?> acceptFollowRequest(@PathVariable Long requestId) {
        social.acceptFollowRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/follow-requests/{requestId}/decline")
    @Transactional
    public ResponseEntity<?> declineFollowRequest(@PathVariable Long requestId) {
        social.declineFollowRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/follow-requests/{requestId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelFollowRequest(@PathVariable Long requestId) {
        social.cancelFollowRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    // ---- BEST FRIEND ----

    @GetMapping("/best-friend-requests/incoming")
    public ResponseEntity<List<FollowRequestView>> bestfriend_incoming() {
        return ResponseEntity.ok(social.getIncomingBestFriendRequests(currentUserId()));
    }

    @GetMapping("/best-friend-requests/outgoing")
    public ResponseEntity<List<FollowRequestView>> bestfriend_outgoing() {
        return ResponseEntity.ok(social.getOutgoingBestFriendRequests(currentUserId()));
    }

    @PostMapping("/best-friend/{targetId}")
    @Transactional
    public ResponseEntity<?> sendBestFriendRequest(@PathVariable Long targetId) {
        Long rid = social.sendBestFriendRequest(currentUserId(), targetId);
        return ResponseEntity.ok().body(rid);
    }

    @PostMapping("/best-friend-requests/{requestId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBestFriend(@PathVariable Long requestId) {
        social.cancelBestFriendRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/best-friend-requests/{requestId}/accept")
    @Transactional
    public ResponseEntity<?> acceptBestFriend(@PathVariable Long requestId) {
        social.acceptBestFriendRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/best-friend-requests/{requestId}/decline")
    @Transactional
    public ResponseEntity<?> declineBestFriend(@PathVariable Long requestId) {
        social.declineBestFriendRequest(currentUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    // Downgrade BEST_FRIEND -> FRIEND (both sides), without unfollowing
    @PostMapping("/best-friend/{targetId}/remove")
    @Transactional
    public ResponseEntity<?> removeBestFriend(@PathVariable Long targetId) {
        social.removeBestFriend(currentUserId(), targetId);
        return ResponseEntity.noContent().build();
    }

    // ---- ACCOUNT PRIVACY ----
    @PostMapping("/me/privacy")
    @Transactional
    public ResponseEntity<?> setPrivacy(@RequestBody PrivacyRequest req) {
        social.setAccountPrivacy(currentUserId(), req.isPrivate());
        return ResponseEntity.noContent().build();
    }
}
