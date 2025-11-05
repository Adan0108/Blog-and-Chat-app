package com.blog.repository.social;

import com.blog.entity.social.FollowRequest;
import com.blog.entity.social.RequestStatus;
import com.blog.entity.social.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest,Long> {

    List<FollowRequest> findByTargetIdAndStatus(Long targetId, RequestStatus status);

    List<FollowRequest> findByRequesterIdAndStatus(Long requester, RequestStatus status);

    List<FollowRequest> findByRequesterIdAndReqTypeAndStatus(Long requesterId, RequestType reqType, RequestStatus status);
    List<FollowRequest> findByTargetIdAndReqTypeAndStatus(Long targetId, RequestType reqType, RequestStatus status);

    boolean existsByRequesterIdAndTargetIdAndReqTypeAndStatus(
            Long requesterId, Long targetId, RequestType reqType, RequestStatus status
    );
}
