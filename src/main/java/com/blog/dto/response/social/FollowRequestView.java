package com.blog.dto.response.social;

public record FollowRequestView(
        Long id,
        Long requesterId,
        Long targetId,
        String type,      // FOLLOW | BEST_FRIEND
        String status,    // "Pending Only"
        String createdAt  // ISO-8601
) {}
