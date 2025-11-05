package com.blog.service.impl.social;

import com.blog.dto.response.social.FollowRequestView;
import com.blog.entity.social.FollowRequest;
import com.blog.entity.social.RelationType;
import com.blog.entity.social.RequestStatus;
import com.blog.entity.social.RequestType;
import com.blog.entity.user.User;
import com.blog.entity.user.UserProfile;
import com.blog.repository.social.FollowRequestRepository;
import com.blog.repository.social.UserFollowersRepository;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.social.SocialService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class SocialServiceImpl implements SocialService {

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final UserFollowersRepository followers;
    private final FollowRequestRepository requests;

    public SocialServiceImpl(UserRepository users,
                             UserProfileRepository profiles,
                             UserFollowersRepository followers,
                             FollowRequestRepository requests) {
        this.users = users;
        this.profiles = profiles;
        this.followers = followers;
        this.requests = requests;
    }

    // ==== helpers ====
    private User getUser(Long id) {
        return users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    private boolean isPrivate(Long userId) {
        return profiles.findByUserId(userId).map(UserProfile::getIsPrivate).orElse(false);
    }

    private void promoteToFriendIfMutual(Long a, Long b) {
        var ab = followers.getRelation(a, b);
        var ba = followers.getRelation(b, a);
        if (ab == RelationType.FOLLOWER && ba == RelationType.FOLLOWER) {
            var now = Instant.now();
            followers.upsertRelation(a, b, RelationType.FRIEND.name(), now);
            followers.upsertRelation(b, a, RelationType.FRIEND.name(), now);
        }
    }

    private FollowRequestView viewOf(FollowRequest fr) {
        return new FollowRequestView(
                fr.getId(),
                fr.getRequester().getId(),
                fr.getTarget().getId(),
                fr.getReqType().name(),
                "PENDING",
                DateTimeFormatter.ISO_INSTANT.format(fr.getCreatedAt())
        );
    }

    // ==== unified follow ====
    @Override
    public void follow(Long me, Long targetId) {
        if (me.equals(targetId)) return;

        // 0) Redundancy guard — if I already have any edge to target, do nothing
        var current = followers.getRelation(me, targetId);
        if (current == RelationType.FOLLOWER || current == RelationType.FRIEND || current == RelationType.BEST_FRIEND) {
            return; // already following at some level
        }

        // Also block if there is an existing pending FOLLOW request (outgoing)
        boolean myPending = requests.existsByRequesterIdAndTargetIdAndReqTypeAndStatus(
                me, targetId, RequestType.FOLLOW, RequestStatus.PENDING);
        if (myPending) return;

        if (isPrivate(targetId)) {
            // Private target: create PENDING request unless there is one incoming from them (not relevant for follow)
            var req = new FollowRequest();
            req.setRequester(getUser(me));
            req.setTarget(getUser(targetId));
            req.setReqType(RequestType.FOLLOW);
            requests.save(req);
            return;
        }

        // Public: create follower edge and maybe promote to FRIEND if mutual
        followers.upsertRelation(me, targetId, RelationType.FOLLOWER.name(), Instant.now());
        promoteToFriendIfMutual(me, targetId);
    }

    @Override
    public void unfollow(Long me, Long targetId) {
        followers.deleteByFollowerIdAndFollowingId(me, targetId);
        var reverse = followers.getRelation(targetId, me);
        if (reverse == RelationType.FRIEND || reverse == RelationType.BEST_FRIEND) {
            followers.upsertRelation(targetId, me, RelationType.FOLLOWER.name(), Instant.now());
        }
    }

    @Override
    public void removeFollower(Long me, Long followerId) {
        followers.deleteByFollowerIdAndFollowingId(followerId, me);
        var mine = followers.getRelation(me, followerId);
        if (mine == RelationType.FRIEND || mine == RelationType.BEST_FRIEND) {
            followers.upsertRelation(me, followerId, RelationType.FOLLOWER.name(), Instant.now());
        }
    }

    // ==== follow requests (DELETE instead of status transitions) ====
    @Override
    public void acceptFollowRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getTarget().getId().equals(me)) throw new IllegalStateException("not your request to accept");

        followers.upsertRelation(req.getRequester().getId(), me, RelationType.FOLLOWER.name(), Instant.now());
        promoteToFriendIfMutual(req.getRequester().getId(), me);

        requests.delete(req); // delete after accept
    }

    @Override
    public void declineFollowRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getTarget().getId().equals(me)) throw new IllegalStateException("not your request to decline");
        requests.delete(req); // delete PENDING
    }

    @Override
    public void cancelFollowRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getRequester().getId().equals(me)) throw new IllegalStateException("not your request to cancel");
        requests.delete(req); // delete PENDING
    }

    @Override
    public List<FollowRequestView> getIncomingFollowRequests(Long me) {
        return requests.findByTargetIdAndReqTypeAndStatus(me, RequestType.FOLLOW, RequestStatus.PENDING)
                .stream().map(this::viewOf).toList();
    }

    @Override
    public List<FollowRequestView> getOutgoingFollowRequests(Long me) {
        return requests.findByRequesterIdAndReqTypeAndStatus(me, RequestType.FOLLOW, RequestStatus.PENDING)
                .stream().map(this::viewOf).toList();
    }

    // ==== best friend (DELETE requests on accept/decline/cancel) ====
    @Override
    public Long sendBestFriendRequest(Long me, Long targetId) {
        // 0) Already BEST_FRIEND? no-op
        if (followers.getRelation(me, targetId) == RelationType.BEST_FRIEND &&
                followers.getRelation(targetId, me) == RelationType.BEST_FRIEND) {
            return null; // already best friends
        }

        // 1) Must be mutual FRIEND
        if (followers.getRelation(me, targetId) != RelationType.FRIEND ||
                followers.getRelation(targetId, me) != RelationType.FRIEND) {
            throw new IllegalStateException("Best-friend requires mutual FRIEND first");
        }

        // 2) If I already sent a PENDING best-friend request, return its id
        var mine = requests.findByRequesterIdAndReqTypeAndStatus(me, RequestType.BEST_FRIEND, RequestStatus.PENDING)
                .stream().filter(r -> r.getTarget().getId().equals(targetId)).findFirst();
        if (mine.isPresent()) return mine.get().getId();

        // 3) If they already sent me a PENDING best-friend request, we could auto-accept or return that id.
        //    Here we return their pending id so the client can accept it.
        var theirs = requests.findByRequesterIdAndReqTypeAndStatus(targetId, RequestType.BEST_FRIEND, RequestStatus.PENDING)
                .stream().filter(r -> r.getTarget().getId().equals(me)).findFirst();
        if (theirs.isPresent()) return theirs.get().getId();

        // 4) Otherwise create new pending request
        var req = new FollowRequest();
        req.setRequester(getUser(me));
        req.setTarget(getUser(targetId));
        req.setReqType(RequestType.BEST_FRIEND);
        req = requests.save(req);
        return req.getId();
    }

    @Override
    public void acceptBestFriendRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getTarget().getId().equals(me)) throw new IllegalStateException("not your request to accept");

        Long a = req.getRequester().getId(), b = me;
        var now = Instant.now();
        followers.upsertRelation(a, b, RelationType.BEST_FRIEND.name(), now);
        followers.upsertRelation(b, a, RelationType.BEST_FRIEND.name(), now);

        requests.delete(req); // delete after accept
    }

    @Override
    public void declineBestFriendRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getTarget().getId().equals(me)) throw new IllegalStateException("not your request to decline");
        requests.delete(req); // delete PENDING
    }

    @Override
    public void cancelBestFriendRequest(Long me, Long requestId) {
        var req = requests.findById(requestId).orElseThrow(() -> new EntityNotFoundException("request not found"));
        if (!req.getRequester().getId().equals(me)) throw new IllegalStateException("not your request to cancel");
        requests.delete(req); // delete PENDING
    }

    @Override
    public List<FollowRequestView> getIncomingBestFriendRequests(Long me) {
        return requests.findByTargetIdAndReqTypeAndStatus(me, RequestType.BEST_FRIEND, RequestStatus.PENDING)
                .stream().map(this::viewOf).toList();
    }

    @Override
    public List<FollowRequestView> getOutgoingBestFriendRequests(Long me) {
        return requests.findByRequesterIdAndReqTypeAndStatus(me, RequestType.BEST_FRIEND, RequestStatus.PENDING)
                .stream().map(this::viewOf).toList();
    }

    // ==== demote best-friend ====
    @Override
    public void removeBestFriend(Long me, Long targetId) {
        var ab = followers.getRelation(me, targetId);
        var ba = followers.getRelation(targetId, me);
        if (ab == RelationType.BEST_FRIEND && ba == RelationType.BEST_FRIEND) {
            var now = Instant.now();
            followers.upsertRelation(me, targetId, RelationType.FRIEND.name(), now);
            followers.upsertRelation(targetId, me, RelationType.FRIEND.name(), now);
        }
    }

    // ==== privacy ====
    @Override
    public void setAccountPrivacy(Long me, boolean isPrivate) {
        var u = getUser(me);
        var p = profiles.findByUserId(me).orElseGet(() -> {
            var np = new UserProfile();
            np.setUser(u);
            return np;
        });
        p.setIsPrivate(isPrivate);
        profiles.save(p);
    }

    @Override
    public boolean getAccountPrivacy(Long me) {
        return profiles.findByUserId(me).map(UserProfile::getIsPrivate).orElse(false);
    }
}
