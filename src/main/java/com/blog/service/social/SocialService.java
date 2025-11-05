package com.blog.service.social;

import com.blog.dto.response.social.FollowRequestView;
import java.util.List;

public interface SocialService {
    // Follow / Unfollow / Kick
    void follow(Long me, Long targetId);              // unified: follow now or create PENDING request
    void unfollow(Long me, Long targetId);
    void removeFollower(Long me, Long followerId);    // "Remove follower" like Instagram

    // Follow requests lifecycle (DELETE rows instead of updating status)
    void acceptFollowRequest(Long me, Long requestId);  // accept then DELETE request
    void declineFollowRequest(Long me, Long requestId); // DELETE request
    void cancelFollowRequest(Long me, Long requestId);  // DELETE request

    // List follow requests (PENDING)
    List<FollowRequestView> getIncomingFollowRequests(Long me);
    List<FollowRequestView> getOutgoingFollowRequests(Long me);

    // Best friend (requires mutual FRIEND)
    Long sendBestFriendRequest(Long me, Long targetId);
    void acceptBestFriendRequest(Long me, Long requestId);  // accept then DELETE request
    void declineBestFriendRequest(Long me, Long requestId); // DELETE request
    void cancelBestFriendRequest(Long me, Long requestId);  // DELETE request

    // List best-friend requests (PENDING)
    List<FollowRequestView> getIncomingBestFriendRequests(Long me);
    List<FollowRequestView> getOutgoingBestFriendRequests(Long me);

    // Demote BEST_FRIEND -> FRIEND (both sides)
    void removeBestFriend(Long me, Long targetId);

    // Privacy (stored on UserProfile)
    void setAccountPrivacy(Long me, boolean isPrivate);
    boolean getAccountPrivacy(Long me);
}
